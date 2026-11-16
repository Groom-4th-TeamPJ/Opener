#!/usr/bin/env python3
"""S1 수집기 - 측정 구간(hold) 시작·끝에 Redis·앱·RabbitMQ·모의 서버를 스냅샷하고 summary.json 을 만든다

watch      : window.json 이 가리키는 hold 구간 동안 5초마다 CPU 를 표집하고 시작·끝 스냅샷을 collector.json 에 쓴다
drain      : 생성기가 끝난 뒤 저장·요약 큐를 5초마다 최대 300초 표집하고 chat_messages 행을 읽어 drain.json 에 쓴다
summarize  : 생성기 원자료(turns-*.jsonl 등) + collector.json + drain.json -> summary.json
사용: collect_s1.py watch --out DIR | collect_s1.py drain --out DIR | collect_s1.py summarize --out DIR --code base|new|A|AB --commit HASH [--app-env "K=V ..."]
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
# 복제본은 마스터 스크립트의 효과를 MULTI ... EXEC 로 감싸 받으므로 multi·exec 도 뺀다 (시험 실측)
NOT_CLIENT = {"info", "replconf", "psync", "sync"}
REPLICA_ONLY_NOT_CLIENT = {"multi", "exec"}
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
        m = re.match(r"cmdstat_(\S+?):calls=(\d+),usec=(\d+)(?:.*failed_calls=(\d+))?", line)
        if m:
            out[m.group(1)] = {"calls": int(m.group(2)), "usec": int(m.group(3)), "failed": int(m.group(4) or 0)}
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


def client_round_trips(role, calls, write_cmds, failed=None):
    """failed: 명령별 실패 횟수 - EVALSHA 가 NOSCRIPT 로 실패하면 앱이 EVAL 로 다시 보내므로 실행 횟수에서 뺀다"""
    failed = failed or {}
    cmds = {c: n for c, n in calls.items() if c not in NOT_CLIENT}
    if role != "master":
        return sum(n for c, n in cmds.items() if c not in write_cmds and c not in REPLICA_ONLY_NOT_CLIENT)
    total = sum(cmds.values())
    evals = calls.get("evalsha", 0) - failed.get("evalsha", 0) + calls.get("eval", 0)
    claim = min(calls.get("hsetnx", 0), evals)
    for script, n in ((SCRIPTS[0], claim), (SCRIPTS[1], evals - claim)):
        for cmd, k in script["internal"].items():
            total -= min(cmds.get(cmd, 0), k * n)
            cmds[cmd] = max(0, cmds.get(cmd, 0) - k * n)
    return total


def vus_peak(samples):
    """측정 구간에서 앱이 센 SSE 연결 수의 최댓값 - 설정한 VU 수가 아니라 실제로 붙어 있던 세션"""
    return int(max([r["sse_active"] for r in samples if r.get("sse_active") is not None] or [0]))


def history_fallback(p0, p1, code):
    """복제본 지연으로 마스터를 다시 읽은 횟수 - Micrometer 카운터는 처음 증가할 때 등록되므로 새 코드에서 없으면 0, 옛 코드는 지표 자체가 없어 None"""
    now = prom_sum(p1, "chat_redis_history_fallback_total")
    if now is None:
        return None if code == "base" else 0
    return int(now - (prom_sum(p0, "chat_redis_history_fallback_total") or 0))


SAVE_Q, SUMMARY_Q = "chat.message.save.queue", "chat.message.summary.queue"
SAVE_DLQ, SUMMARY_DLQ = "chat.message.save.dlq", "chat.message.summary.dlq"
DRAIN_MAX_S = 300


def parse_queue_depths(text):
    """rabbitmqctl list_queues name messages 출력 -> {큐 이름: 깊이}"""
    out = {}
    for line in text.splitlines():
        parts = line.split()
        if len(parts) == 2 and parts[1].isdigit():
            out[parts[0]] = int(parts[1])
    return out


def depth_pair(depths):
    """저장·요약 큐와 각 DLQ 의 깊이 - 요약 큐는 B 이전 코드에 없으므로 None"""
    return {"save": depths.get(SAVE_Q), "summary": depths.get(SUMMARY_Q),
            "save_dlq": depths.get(SAVE_DLQ), "summary_dlq": depths.get(SUMMARY_DLQ)}


def drain_seconds(series, step=SAMPLE_S):
    """저장·요약 큐가 모두 처음 빈 표본의 순번 x 간격(초), 끝까지 안 비면 None - 없는 큐(None)는 빈 것으로 본다"""
    for k, d in enumerate(series):
        if not (d.get("save") or 0) and not (d.get("summary") or 0):
            return k * step
    return None


def clock_offset_ms(host_before, host_after, db_epoch_s):
    """DB 가 답한 시각 - 호스트 왕복 중간 시각 (밀리초) - 도커 안 psql 왕복 시간의 절반이 오차"""
    return round((db_epoch_s - (host_before + host_after) / 2) * 1000)


def parse_rows(text, host_gmtoff_s):
    """psql -At 행 'qrid|created_epoch|updated_epoch|summary_null' -> 호스트 시계 기준 밀리초
    created_at 은 시간대 없는 timestamp 라 UTC 로 읽은 에포크가 오므로 호스트 UTC 오프셋을 뺀다 (앱 JVM 이 호스트 시간대의 LocalDateTime 을 쓴다)"""
    rows = []
    for line in text.splitlines():
        parts = line.strip().split("|")
        if len(parts) != 4:
            continue
        rows.append({
            "qrid": int(parts[0]), "created_ms": (float(parts[1]) - host_gmtoff_s) * 1000,
            "updated_ms": (float(parts[2]) - host_gmtoff_s) * 1000 if parts[2] else None, "summary_null": parts[3] == "t",
        })
    return rows


def ms_stats(values):
    return {"p50": percentile(values, 50), "p95": percentile(values, 95), "count": len(values)}


def save_summary_blocks(posts, rows, offset_ms):
    """posts: {question_result_id: 저장 요청 전송 시각(호스트 ms)}, rows: parse_rows 결과, offset_ms: DB 시계 - 호스트 시계"""
    created = {r["qrid"]: r["created_ms"] for r in rows}
    durable = [created[q] - offset_ms - sent for q, sent in posts.items() if q in created]
    # 요약과 함께 저장된 행은 updated_at 이 없거나 created_at 과 같아 지연 0, 나중에 채운 행은 둘의 차이
    lags = [max(0.0, (r["updated_ms"] or r["created_ms"]) - r["created_ms"]) for r in rows if not r["summary_null"]]
    missing = sum(1 for r in rows if r["summary_null"])
    not_durable = sum(1 for q in posts if q not in created)
    save = {"rows": len(rows), "posts": len(posts), "not_durable": not_durable, "durable_ms": ms_stats(durable)}
    summary = {"lag_ms": ms_stats(lags), "missing_at_end": missing, "missing_incl_unsaved": missing + not_durable}
    return save, summary


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
        "t": time.time(), "role": rep.get("role"),
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
        "depths_at_hold_end": rabbit_depths(sc),
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


def rabbit_depths(sc):
    """정확한 큐 깊이 - 관리 API 의 messages 는 5초 간격 갱신이라 뒤처지므로 rabbitmqctl 을 쓰고, 실패하면 관리 API"""
    out = subprocess.run(["docker", "exec", "r125s1-rabbitmq", "rabbitmqctl", "list_queues", "name", "messages", "--quiet"],
                         capture_output=True, text=True)
    depths = parse_queue_depths(out.stdout) if out.returncode == 0 else {}
    if not depths:
        queues = http_json(f"http://{HOST}:{sc['ports']['rabbit_mgmt']}/api/queues", "guest:guest") or []
        depths = {q["name"]: q.get("messages", 0) for q in queues}
    return depth_pair(depths)


def psql_text(sql):
    out = subprocess.run(["docker", "exec", "-i", "r125s1-db", "psql", "-U", "dev", "-d", "opener_s1", "-At", "-c", sql],
                         capture_output=True, text=True)
    if out.returncode != 0:
        raise RuntimeError("psql 실패: " + out.stderr.strip())
    return out.stdout


def measure_clock_offset_ms():
    before = time.time()
    db = float(psql_text("SELECT EXTRACT(EPOCH FROM clock_timestamp())").strip())
    return clock_offset_ms(before, time.time(), db)


def drain(out, sc):
    """생성기가 끝난 직후부터 큐를 5초마다 최대 300초 표집하고, 끝나면 이번 회차의 chat_messages 행을 읽는다"""
    w = json.load(open(os.path.join(out, "window.json")))
    series, nxt = [], time.time()
    while True:
        row = {"t": time.time(), **rabbit_depths(sc)}
        series.append(row)
        if drain_seconds([row]) is not None or len(series) > DRAIN_MAX_S // SAMPLE_S:
            break
        nxt += SAMPLE_S
        time.sleep(max(0.0, nxt - time.time()))
    offset = measure_clock_offset_ms()
    gmtoff = time.localtime().tm_gmtoff
    # 회차 시작(t0) 이후 만들어진 행만 - 시드 템플릿에 있던 행은 제외, 시작 전 10초 여유
    since = (w["t0"] - 10) + gmtoff
    sql = ("SELECT question_result_id, EXTRACT(EPOCH FROM created_at), COALESCE(EXTRACT(EPOCH FROM updated_at)::text, ''), "
           "CASE WHEN summary IS NULL THEN 't' ELSE 'f' END FROM chat_messages WHERE EXTRACT(EPOCH FROM created_at) >= %f" % since)
    text = psql_text(sql)
    json.dump({"series": series, "end": rabbit_depths(sc), "clock_offset_ms": offset, "host_gmtoff_s": gmtoff, "rows_raw": text},
              open(os.path.join(out, "drain.json"), "w"))


# ---------- 요약 ----------
def delta_calls(a, b):
    return {c: v["calls"] - a.get(c, {}).get("calls", 0) for c, v in b.items() if v["calls"] - a.get(c, {}).get("calls", 0)}


def node_summary(s0, s1, write_cmds, idle_us_per_s=0.0):
    calls = delta_calls(s0["commandstats"], s1["commandstats"])
    failed = {c: v.get("failed", 0) - s0["commandstats"].get(c, {}).get("failed", 0) for c, v in s1["commandstats"].items()}
    cpu_us = round((s1["cpu_sys"] + s1["cpu_user"] - s0["cpu_sys"] - s0["cpu_user"]) * 1e6)
    return {
        "role": s1["role"], "calls": calls,
        "client_round_trips": client_round_trips("master" if s1["role"] == "master" else "slave", calls, set(write_cmds), failed),
        "cpu_us": cpu_us, "cpu_us_net": round(cpu_us - idle_us_per_s * (s1["t"] - s0["t"])),
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


def idle_baseline(out, sc, seconds):
    """부하 없이 노드가 쓰는 CPU(AOF·클러스터 버스·복제 핑) - 부하 CPU 에서 빼는 값"""
    ports = [*sc["ports"]["redis_chat"], sc["ports"]["redis_auth"]]
    a = {p: node_snapshot(p) for p in ports}
    time.sleep(seconds)
    b = {p: node_snapshot(p) for p in ports}
    res = {str(p): ((b[p]["cpu_sys"] + b[p]["cpu_user"] - a[p]["cpu_sys"] - a[p]["cpu_user"]) * 1e6) / (b[p]["t"] - a[p]["t"])
           for p in ports}
    json.dump(res, open(os.path.join(out, "idle.json"), "w"))


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


def summarize(out, code, commit, sc, app_env=None):
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
    idle_path = os.path.join(out, "idle.json")
    idle = json.load(open(idle_path)) if os.path.exists(idle_path) else {}
    nodes = {p: node_summary(s0["chat"][p], s1["chat"][p], col["write_cmds"], idle.get(p, 0.0)) for p in s1["chat"]}
    agg = {r: [n for n in nodes.values() if (n["role"] == "master") == (r == "master")] for r in ("master", "replica")}

    def per_turn(key, role):
        return sum(n[key] for n in agg[role]) / n_turns if n_turns else None

    auth = node_summary(s0["auth"], s1["auth"], col["write_cmds"], idle.get(str(sc["ports"]["redis_auth"]), 0.0))
    p0, p1 = prom_from_snapshot(s0["prom"]), prom_from_snapshot(s1["prom"])
    fallback = history_fallback(p0, p1, code)
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
    q0, q1 = s0["rabbit"].get(SAVE_Q, {}), s1["rabbit"].get(SAVE_Q, {})
    sq0, sq1 = s0["rabbit"].get(SUMMARY_Q, {}), s1["rabbit"].get(SUMMARY_Q, {})
    dr_path = os.path.join(out, "drain.json")
    dr = json.load(open(dr_path)) if os.path.exists(dr_path) else None
    rows = parse_rows(dr["rows_raw"], dr["host_gmtoff_s"]) if dr else []
    # 같은 결과 id 로 저장 요청이 둘이면 먼저 보낸 것 - 행은 결과 id 당 하나뿐
    posts = {}
    for sess in sorted((x for x in sessions if x.get("save_code") == 200), key=lambda x: x["save_sent_ms"]):
        posts.setdefault(sess["question_result_id"], sess["save_sent_ms"])
    save_block, summary_block = save_summary_blocks(posts, rows, dr["clock_offset_ms"] if dr else 0)
    save_block["clock_offset_ms"] = dr["clock_offset_ms"] if dr else None
    summary_block["queue_published"] = (sq1.get("stats", {}).get("publish", 0)) - (sq0.get("stats", {}).get("publish", 0)) if sq1 else None
    summary_block["queue_consumed"] = (sq1.get("stats", {}).get("ack", 0)) - (sq0.get("stats", {}).get("ack", 0)) if sq1 else None
    depth_end = depth_pair({k: v["messages"] for k, v in s1["rabbit"].items()})
    if "depths_at_hold_end" in s1:
        depth_end = s1["depths_at_hold_end"]
    final_rabbit = http_json(f"http://{HOST}:{sc['ports']['rabbit_mgmt']}/api/queues", "guest:guest") or []
    final_mock = http_json(f"http://{HOST}:{sc['ports']['mock']}/stats")
    sess_done = [s for s in sessions if s.get("ok") and s.get("t_end") and window[0] <= s["t_end"] <= window[1]]
    summary = {
        "code": code, "commit": commit,
        "vus_peak": vus_peak(samples), "vus_configured": w["vus"], "login_failed_vus": len(login_failed), "hold_s": w["hold_s"],
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
            "master_cpu_us_per_turn_net": per_turn("cpu_us_net", "master"),
            "replica_cpu_us_per_turn_net": per_turn("cpu_us_net", "replica"),
            "idle_cpu_us_per_s": idle,
            "master_ops_per_s": sum(n["client_round_trips"] for n in agg["master"]) / dur,
            "replica_ops_per_s": sum(n["client_round_trips"] for n in agg["replica"]) / dur,
            "nodes": nodes, "auth": auth, "rule": RULE_TEXT,
        },
        "app": {
            "history_fallback": fallback,
            "history_fallback_per_turn": None if fallback is None or not n_turns else fallback / n_turns,
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
            "depth_at_hold_end": {"save": depth_end["save"], "summary": depth_end["summary"]},
            "dlq_at_hold_end": {"save": depth_end["save_dlq"], "summary": depth_end["summary_dlq"]},
            "drain_s": drain_seconds(dr["series"]) if dr else None,
            "drain_watch_s": (len(dr["series"]) - 1) * SAMPLE_S if dr else None,
            "dlq": {"save": dr["end"]["save_dlq"], "summary": dr["end"]["summary_dlq"]} if dr else None,
            "depth_at_drain_end": {"save": dr["end"]["save"], "summary": dr["end"]["summary"]} if dr else None,
        },
        "save": save_block,
        "summary": summary_block,
        "app_env": app_env,
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
    ap.add_argument("mode", choices=["watch", "drain", "summarize", "idle"])
    ap.add_argument("--seconds", type=float, default=20)
    ap.add_argument("--out", required=True)
    ap.add_argument("--scenario", default=os.path.join(HERE, "s1", "scenario.json"))
    ap.add_argument("--code")
    ap.add_argument("--commit")
    ap.add_argument("--app-env", default="")
    args = ap.parse_args()
    sc = json.load(open(args.scenario, encoding="utf-8"))
    if args.mode == "watch":
        watch(args.out, sc)
    elif args.mode == "drain":
        drain(args.out, sc)
    elif args.mode == "idle":
        idle_baseline(args.out, sc, args.seconds)
    else:
        summarize(args.out, args.code, args.commit, sc, dict(kv.split("=", 1) for kv in args.app_env.split() if "=" in kv))


if __name__ == "__main__":
    main()
