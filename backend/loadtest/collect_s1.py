#!/usr/bin/env python3
"""S1 수집기 - 측정 구간(hold) 시작·끝에 Redis·앱·RabbitMQ·모의 서버를 스냅샷하고 summary.json 을 만든다

watch      : window.json 이 가리키는 hold 구간 동안 5초마다 CPU 를 표집하고 시작·끝 스냅샷을 collector.json 에 쓴다
summarize  : 생성기 원자료(turns-*.jsonl 등) + collector.json -> summary.json
사용: collect_s1.py watch --out DIR | collect_s1.py summarize --out DIR --code base|new --commit HASH [--tiny]
"""
import argparse
import base64
import glob
import json
import os
import platform
import re
import socket
import subprocess
import sys
import time
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from s1_load import percentile  # noqa: E402

SAMPLE_S = 5
HOST = "127.0.0.1"
# 클라이언트 왕복으로 세지 않는 명령 - info 는 수집기 자신, replconf·psync·sync 는 복제 연결
NOT_CLIENT = {"info", "replconf", "psync", "sync"}
# 스크립트 한 번이 Redis 안에서 실행하는 명령 수 - Redis 는 이것도 commandstats 에 센다 (시험 실측: EVAL 1회 -> eval 1 + 내부 명령 각 1)
# calls_from: 스크립트 호출 횟수를 읽을 곳 - claim 은 호출마다 HSETNX 를 1번 실행하고 앱이 직접 보내지 않는다
# save(RPUSH + EXPIRE 2번)는 evalsha 호출 중 claim 이 아닌 나머지
# shortcut: 스크립트가 새로 생기면 이 표에 행을 더해야 한다, 표에 없으면 내부 명령이 왕복으로 더해져 과다 집계
SCRIPTS = [
    {"name": "claim", "internal": {"exists": 1, "hsetnx": 1, "hset": 1, "expire": 1}},
    {"name": "save", "internal": {"rpush": 1, "expire": 2}},
]
RULE_TEXT = ("왕복 = commandstats calls 합 - info·replconf·psync·sync - 스크립트 내부 명령"
             "(claim: hsetnx 호출 수만큼 exists·hsetnx·hset·expire 각 1, save: evalsha 중 나머지 호출 수만큼 rpush 1·expire 2); "
             "복제본은 쓰기 명령(복제 스트림으로 들어온 것)을 뺀 나머지")


# ---------- 순수 함수 ----------
def parse_commandstats(text):
    out = {}
    for line in text.splitlines():
        m = re.match(r"cmdstat_(\S+?):calls=(\d+),usec=(\d+)", line)
        if m:
            out[m.group(1)] = {"calls": int(m.group(2)), "usec": int(m.group(3))}
    return out


def parse_prom(text):
    out = {}
    for line in text.splitlines():
        if not line or line[0] == "#":
            continue
        m = re.match(r"([a-zA-Z_:][a-zA-Z0-9_:]*)(?:\{(.*)\})?\s+(\S+)", line)
        if not m:
            continue
        labels = tuple(sorted(re.findall(r'(\w+)="([^"]*)"', m.group(2) or "")))
        try:
            out[(m.group(1), labels)] = float(m.group(3))
        except ValueError:
            pass
    return out


def _match(labels, want):
    d = dict(labels)
    return all(d.get(k) == v for k, v in want.items())


def prom_value(prom, name, **labels):
    for (n, lb), v in prom.items():
        if n == name and _match(lb, labels):
            return v
    return None


def prom_sum(prom, name, **labels):
    vals = [v for (n, lb), v in prom.items() if n == name and _match(lb, labels)]
    return sum(vals) if vals else None


def prom_series(prom, name, **labels):
    return [(dict(lb), v) for (n, lb), v in prom.items() if n == name and _match(lb, labels)]


def parse_ps_time(text):
    days = 0
    if "-" in text:
        d, text = text.split("-", 1)
        days = int(d)
    parts = [float(p) for p in text.split(":")]
    secs = 0.0
    for p in parts:
        secs = secs * 60 + p
    return days * 86400 + secs


def quantile_from_buckets(buckets, q):
    """누적 버킷 {상한: 누적 개수} 에서 분위수 - 버킷 안은 선형 보간"""
    items = sorted(buckets.items())
    total = items[-1][1] if items else 0
    if not total:
        return None
    target, prev_le, prev_cum = q * total, 0.0, 0.0
    for le, cum in items:
        if cum >= target:
            if le == float("inf") or cum == prev_cum:
                return prev_le
            return prev_le + (target - prev_cum) / (cum - prev_cum) * (le - prev_le)
        prev_le, prev_cum = le, cum
    return prev_le


def client_round_trips(role, calls, write_cmds):
    cmds = {c: n for c, n in calls.items() if c not in NOT_CLIENT}
    if role != "master":
        return sum(n for c, n in cmds.items() if c not in write_cmds)
    total = sum(cmds.values())
    evals = calls.get("evalsha", 0) + calls.get("eval", 0)
    claim = min(calls.get("hsetnx", 0), evals)
    for script, n in ((SCRIPTS[0], claim), (SCRIPTS[1], evals - claim)):
        for cmd, k in script["internal"].items():
            total -= min(cmds.get(cmd, 0), k * n)
            cmds[cmd] = max(0, cmds.get(cmd, 0) - k * n)
    return total


def pct3(values):
    return {"p50": percentile(values, 50), "p95": percentile(values, 95), "p99": percentile(values, 99)}


def turn_stats(turns):
    window = [t for t in turns if t["in_window"]]
    ok = [t for t in window if t["ok"]]
    return {
        "turns_completed": len(ok), "attempts_in_window": len(window),
        "ttft_ms": pct3([t["ttft_ms"] for t in ok if t["ttft_ms"] is not None]),
        "turn_ms": pct3([t["turn_ms"] for t in ok]),
    }


def errors_rate(errors, window, attempts):
    if not attempts:
        return None
    return sum(1 for e in errors if window[0] <= e["t"] <= window[1]) / attempts


# ---------- 수집 ----------
def resp_read(f):
    line = f.readline()
    kind, rest = line[:1], line[1:-2]
    if kind == b"$":
        n = int(rest)
        return None if n < 0 else f.read(n + 2)[:-2].decode("utf-8", "replace")
    if kind == b"*":
        return [resp_read(f) for _ in range(int(rest))]
    if kind == b":":
        return int(rest)
    if kind == b"+":
        return rest.decode()
    raise RuntimeError(f"redis error: {rest.decode(errors='replace')}")


def redis_cmd(port, *args):
    with socket.create_connection((HOST, port), timeout=10) as s:
        payload = b"*%d\r\n" % len(args) + b"".join(b"$%d\r\n%s\r\n" % (len(a.encode()), a.encode()) for a in args)
        s.sendall(payload)
        return resp_read(s.makefile("rb"))


def info_fields(text):
    return dict(l.split(":", 1) for l in text.splitlines() if ":" in l and not l.startswith("#"))


def node_snapshot(port):
    rep = info_fields(redis_cmd(port, "INFO", "replication"))
    cpu = info_fields(redis_cmd(port, "INFO", "cpu"))
    stats = info_fields(redis_cmd(port, "INFO", "stats"))
    return {
        "role": rep.get("role"),
        "commandstats": parse_commandstats(redis_cmd(port, "INFO", "commandstats")),
        "cpu_sys": float(cpu["used_cpu_sys"]), "cpu_user": float(cpu["used_cpu_user"]),
        "total_commands": int(stats["total_commands_processed"]),
    }


PROM_KEEP = ("chat_", "rag_vector", "jvm_threads", "process_cpu", "system_cpu", "resilience4j_circuitbreaker_state",
             "hikaricp_connections", "tomcat_threads", "jvm_gc_pause_seconds_count", "jvm_gc_pause_seconds_sum")


def prom_snapshot(app_port):
    try:
        with urllib.request.urlopen(f"http://{HOST}:{app_port}/api/actuator/prometheus", timeout=10) as r:
            prom = parse_prom(r.read().decode("utf-8", "replace"))
    except OSError:
        return []
    return [[n, dict(lb), v] for (n, lb), v in prom.items() if n.startswith(PROM_KEEP)]


def prom_from_snapshot(rows):
    return {(n, tuple(sorted(lb.items()))): v for n, lb, v in rows}


def http_json(url, auth=None, timeout=10):
    req = urllib.request.Request(url)
    if auth:
        req.add_header("Authorization", "Basic " + base64.b64encode(auth.encode()).decode())
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return json.loads(r.read())
    except (OSError, ValueError):
        return None


def snapshot(sc):
    ports = sc["ports"]
    chat = {str(p): node_snapshot(p) for p in ports["redis_chat"]}
    auth = node_snapshot(ports["redis_auth"])
    queues = http_json(f"http://{HOST}:{ports['rabbit_mgmt']}/api/queues", "guest:guest") or []
    return {
        "t": time.time(), "chat": chat, "auth": auth,
        "prom": prom_snapshot(ports["app"]),
        "rabbit": {q["name"]: {"messages": q.get("messages", 0), "stats": q.get("message_stats", {})} for q in queues},
        "mock": http_json(f"http://{HOST}:{ports['mock']}/stats"),
    }


def read_pids(path):
    try:
        return [int(x) for x in open(path).read().split()]
    except (OSError, ValueError):
        return []


def proc_cpu_seconds(pids):
    if not pids:
        return {}
    out = subprocess.run(["ps", "-o", "pid=,time=", "-p", ",".join(map(str, pids))], capture_output=True, text=True).stdout
    return {int(l.split()[0]): parse_ps_time(l.split()[1]) for l in out.splitlines() if l.strip()}


def top_cpu_line():
    out = subprocess.run(["top", "-l", "2", "-n", "0", "-s", "1"], capture_output=True, text=True).stdout
    lines = [l for l in out.splitlines() if l.startswith("CPU usage")]
    return lines[-1] if lines else None


def watch(out, sc):
    wpath = os.path.join(out, "window.json")
    for _ in range(600):
        if os.path.exists(wpath):
            break
        time.sleep(0.5)
    w = json.load(open(wpath))
    write_cmds = [c.lower() for c in (redis_cmd(sc["ports"]["redis_chat"][0], "COMMAND", "LIST", "FILTERBY", "ACLCAT", "write") or [])]
    time.sleep(max(0.0, w["hold_start"] - time.time()))
    snap0 = snapshot(sc)
    samples, last = [], {}
    nxt = time.time()
    while True:
        procs = {"gen": read_pids(os.path.join(out, "gen-pids.txt")), "app": read_pids(os.path.join(out, "app.pid")),
                 "mock": read_pids(os.path.join(out, "mock.pid"))}
        now = time.time()
        cpu = proc_cpu_seconds(sum(procs.values(), []))
        row = {"t": now, "top": top_cpu_line()}
        for group, pids in procs.items():
            per = []
            for pid in pids:
                if pid in cpu and pid in last:
                    per.append((cpu[pid] - last[pid][0]) / max(1e-6, now - last[pid][1]) * 100)
            row[group + "_cpu_pct"] = per
        last = {pid: (v, now) for pid, v in cpu.items()}
        prom = prom_from_snapshot(prom_snapshot(sc["ports"]["app"]))
        row["jvm_threads_live"] = prom_value(prom, "jvm_threads_live_threads")
        row["process_cpu_usage"] = prom_value(prom, "process_cpu_usage")
        row["sse_active"] = prom_value(prom, "chat_sse_active_connections")
        row["hikari_active"] = prom_value(prom, "hikaricp_connections_active")
        samples.append(row)
        nxt += SAMPLE_S
        if nxt >= w["hold_end"]:
            break
        time.sleep(max(0.0, nxt - time.time()))
    time.sleep(max(0.0, w["hold_end"] - time.time()))
    snap1 = snapshot(sc)
    json.dump({"window": w, "snap0": snap0, "snap1": snap1, "samples": samples, "write_cmds": write_cmds},
              open(os.path.join(out, "collector.json"), "w"))


# ---------- 요약 ----------
def delta_calls(a, b):
    return {c: v["calls"] - a.get(c, {}).get("calls", 0) for c, v in b.items() if v["calls"] - a.get(c, {}).get("calls", 0)}


def node_summary(s0, s1, write_cmds):
    calls = delta_calls(s0["commandstats"], s1["commandstats"])
    return {
        "role": s1["role"], "calls": calls,
        "client_round_trips": client_round_trips("master" if s1["role"] == "master" else "slave", calls, set(write_cmds)),
        "cpu_us": round((s1["cpu_sys"] + s1["cpu_user"] - s0["cpu_sys"] - s0["cpu_user"]) * 1e6),
    }


def timer_delta(p0, p1, base, **labels):
    c0, c1 = prom_sum(p0, base + "_count", **labels), prom_sum(p1, base + "_count", **labels)
    s0, s1 = prom_sum(p0, base + "_sum", **labels), prom_sum(p1, base + "_sum", **labels)
    if c1 is None:
        return None
    n = c1 - (c0 or 0)
    out = {"count": int(n), "mean_ms": ((s1 - (s0 or 0)) / n * 1000) if n else None, "p95_ms": None}

    def buckets(prom):
        agg = {}
        for d, v in prom_series(prom, base + "_bucket", **labels):
            le = float("inf") if d["le"] == "+Inf" else float(d["le"])
            agg[le] = agg.get(le, 0) + v
        return agg

    b0, b1 = buckets(p0), buckets(p1)
    q = quantile_from_buckets({le: v - b0.get(le, 0) for le, v in b1.items()}, 0.95)
    out["p95_ms"] = q * 1000 if q is not None else None
    return out


def host_info():
    def sh(*cmd):
        return subprocess.run(cmd, capture_output=True, text=True).stdout.strip()
    return {"os": platform.platform(), "cpus": os.cpu_count(), "mem_gb": round(int(sh("sysctl", "-n", "hw.memsize") or 0) / 2 ** 30, 1),
            "python": platform.python_version(), "docker_cpus": sh("docker", "info", "--format", "{{.NCPU}}"),
            "docker_mem_gb": round(int(sh("docker", "info", "--format", "{{.MemTotal}}") or 0) / 2 ** 30, 1)}


def read_jsonl(out, prefix):
    rows = []
    for p in sorted(glob.glob(os.path.join(out, prefix + "-*.jsonl"))):
        rows += [json.loads(l) for l in open(p) if l.strip()]
    return rows


def summarize(out, code, commit, sc):
    col = json.load(open(os.path.join(out, "collector.json")))
    w, s0, s1, samples = col["window"], col["snap0"], col["snap1"], col["samples"]
    turns, sessions, errors = read_jsonl(out, "turns"), read_jsonl(out, "sessions"), read_jsonl(out, "errors")
    window = (w["hold_start"], w["hold_end"])
    ts = turn_stats(turns)
    n_turns = ts["turns_completed"]
    dur = max(1e-6, s1["t"] - s0["t"])
    login_failed = {e["vu"] for e in errors if e["kind"] == "login_failed"}
    by_kind = {}
    for e in errors:
        by_kind[e["kind"]] = by_kind.get(e["kind"], 0) + 1
    nodes = {p: node_summary(s0["chat"][p], s1["chat"][p], col["write_cmds"]) for p in s1["chat"]}
    agg = {r: [n for n in nodes.values() if (n["role"] == "master") == (r == "master")] for r in ("master", "replica")}

    def per_turn(key, role):
        return sum(n[key] for n in agg[role]) / n_turns if n_turns else None

    auth = node_summary(s0["auth"], s1["auth"], col["write_cmds"])
    p0, p1 = prom_from_snapshot(s0["prom"]), prom_from_snapshot(s1["prom"])
    fb1 = prom_sum(p1, "chat_redis_history_fallback_total")
    routes = {}
    for d, _ in prom_series(p1, "chat_redis_read_seconds_count"):
        routes[d.get("route")] = timer_delta(p0, p1, "chat_redis_read_seconds", route=d.get("route"))
    cb = {}
    for d, v in prom_series(p1, "resilience4j_circuitbreaker_state"):
        if v == 1.0:
            cb[d.get("name")] = d.get("state")
    gen_sum = [sum(r["gen_cpu_pct"]) for r in samples if r["gen_cpu_pct"]]
    gen_max_proc = [max(r["gen_cpu_pct"]) for r in samples if r["gen_cpu_pct"]]
    cpu_vals = [r["process_cpu_usage"] for r in samples if r["process_cpu_usage"] is not None]
    app_ps = [sum(r["app_cpu_pct"]) for r in samples if r["app_cpu_pct"]]
    q0, q1 = s0["rabbit"].get("chat.message.save.queue", {}), s1["rabbit"].get("chat.message.save.queue", {})
    final_rabbit = http_json(f"http://{HOST}:{sc['ports']['rabbit_mgmt']}/api/queues", "guest:guest") or []
    final_mock = http_json(f"http://{HOST}:{sc['ports']['mock']}/stats")
    sess_done = [s for s in sessions if s.get("ok") and s.get("t_end") and window[0] <= s["t_end"] <= window[1]]
    summary = {
        "code": code, "commit": commit,
        "vus_peak": w["vus"] - len(login_failed), "hold_s": w["hold_s"],
        "sessions_completed": len(sess_done), "turns_completed": n_turns,
        "turns_per_s": n_turns / w["hold_s"],
        "ttft_ms": ts["ttft_ms"], "turn_ms": ts["turn_ms"],
        "errors_rate": errors_rate(errors, window, ts["attempts_in_window"]),
        "errors_by_kind": by_kind,
        "redis": {
            "master_round_trips_per_turn": per_turn("client_round_trips", "master"),
            "replica_round_trips_per_turn": per_turn("client_round_trips", "replica"),
            "master_cpu_us_per_turn": per_turn("cpu_us", "master"),
            "replica_cpu_us_per_turn": per_turn("cpu_us", "replica"),
            "master_ops_per_s": sum(n["client_round_trips"] for n in agg["master"]) / dur,
            "replica_ops_per_s": sum(n["client_round_trips"] for n in agg["replica"]) / dur,
            "nodes": nodes, "auth": auth, "rule": RULE_TEXT,
        },
        "app": {
            "history_fallback": None if fb1 is None else fb1 - (prom_sum(p0, "chat_redis_history_fallback_total") or 0),
            "redis_read_by_route": routes,
            "sse_ttft_server_ms": timer_delta(p0, p1, "chat_sse_ttft_seconds"),
            "rag_search_ms": timer_delta(p0, p1, "rag_vector_search_seconds", path="chat"),
            "jvm_threads_live_peak": max([r["jvm_threads_live"] for r in samples if r["jvm_threads_live"] is not None] or [None]),
            "process_cpu_mean": sum(cpu_vals) / len(cpu_vals) if cpu_vals else None,
            "circuit_states": cb,
            "app_cpu_ps_mean_pct": sum(app_ps) / len(app_ps) if app_ps else None,
            "sse_active_peak": max([r["sse_active"] for r in samples if r["sse_active"] is not None] or [None]),
            "hikari_active_peak": max([r["hikari_active"] for r in samples if r["hikari_active"] is not None] or [None]),
        },
        "rabbit": {
            "published": (q1.get("stats", {}).get("publish", 0)) - (q0.get("stats", {}).get("publish", 0)),
            "consumed": (q1.get("stats", {}).get("ack", 0)) - (q0.get("stats", {}).get("ack", 0)),
            "dlq_depth_end": s1["rabbit"].get("chat.message.save.dlq", {}).get("messages", 0),
            "dlq_depth_after_run": next((q.get("messages", 0) for q in final_rabbit if q["name"] == "chat.message.save.dlq"), None),
            "save_queue_depth_after_run": next((q.get("messages", 0) for q in final_rabbit if q["name"] == "chat.message.save.queue"), None),
        },
        "mock": final_mock,
        "mock_hold_delta": {k: (s1["mock"] or {}).get(k, 0) - (s0["mock"] or {}).get(k, 0)
                            for k in ("chat_streams", "chat_nonstream", "embeddings", "prompt_checked", "prompt_missing")},
        "loadgen_cpu_max_pct": max(gen_sum or [0.0]), "loadgen_cpu_max_per_proc_pct": max(gen_max_proc or [0.0]),
        "loadgen_cpu_mean_pct": sum(gen_sum) / len(gen_sum) if gen_sum else None,
        "loadgen_cores": w["procs"],
        "mock_cpu_max_pct": max([sum(r["mock_cpu_pct"]) for r in samples if r["mock_cpu_pct"]] or [0.0]),
        "host": host_info(),
        "sessions_completed_total": sum(1 for s in sessions if s.get("ok")),
        "attempts_in_window": ts["attempts_in_window"], "window": {"hold_start": w["hold_start"], "hold_end": w["hold_end"],
                                                                    "snapshot_span_s": dur},
        "errors_rate_def": "측정 구간 안 오류 수 / 측정 구간에 끝난 턴 시도 수(성공·실패 모두)",
        "top_samples": [r["top"] for r in samples if r.get("top")][:: max(1, len(samples) // 12)],
    }
    json.dump(summary, open(os.path.join(out, "summary.json"), "w"), ensure_ascii=False, indent=2)
    return summary


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("mode", choices=["watch", "summarize"])
    ap.add_argument("--out", required=True)
    ap.add_argument("--scenario", default=os.path.join(HERE, "s1", "scenario.json"))
    ap.add_argument("--code")
    ap.add_argument("--commit")
    args = ap.parse_args()
    sc = json.load(open(args.scenario, encoding="utf-8"))
    if args.mode == "watch":
        watch(args.out, sc)
    else:
        summarize(args.out, args.code, args.commit, sc)


if __name__ == "__main__":
    main()
