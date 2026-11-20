#!/usr/bin/env python3
"""S1 수집기 - 측정 구간(hold) 시작·끝에 Redis·앱·RabbitMQ·모의 서버를 스냅샷하고 summary.json 을 만든다

watch      : window.json 이 가리키는 hold 구간 동안 5초마다 CPU 를 표집하고 시작·끝 스냅샷을 collector.json 에 쓴다
drain      : 생성기가 끝난 뒤 저장·요약 큐를 5초마다 최대 300초 표집하고 chat_messages 행을 읽어 drain.json 에 쓴다
fault      : 장애 구간을 걸고 차단기 상태를 1초마다 표집해 circuit.json 에 쓴다 (run.sh 가 S1_FAULT 일 때만 부른다)
             --kind mock(기본, 모의 서버가 Claude·Voyage 경로를 500 으로 답함) | master-stop(채팅 클러스터 마스터 하나를 docker stop 했다가 start)
             | cluster-pause(채팅 클러스터 여섯 노드를 모두 docker pause 했다가 unpause)
             | mock-embeddings(모의 서버가 Voyage 임베딩 경로만 500 으로 답함 - 검색 실패가 답 실패로 번지는지 본다)
share      : share --share-out FILE 폴더... -> 폴더 이름별 {attempted, failed, share} JSON 한 개 (실패 턴 / 측정 구간에 끝난 턴 시도 수)
summarize  : 생성기 원자료(turns-*.jsonl 등) + collector.json + drain.json -> summary.json
사용: collect_s1.py watch --out DIR | collect_s1.py fault --out DIR [--tiny] [--kind mock|mock-embeddings|master-stop|cluster-pause] | collect_s1.py drain --out DIR | collect_s1.py summarize --out DIR --code base|new|A|AB --commit HASH [--app-env "K=V ..."]
"""
import argparse
import base64
import collections
import glob
import json
import math
import os
import platform
import re
import socket
import statistics
import subprocess
import sys
import threading
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


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def dump_json(path, obj, **kw):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, **kw)


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


def prom_max(prom, name, **labels):
    """같은 이름 계열 중 가장 큰 값 - 풀이 둘 이상이어도 가장 나쁜 쪽"""
    vals = [v for (n, lb), v in prom.items() if n == name and _match(lb, labels)]
    return max(vals) if vals else None


def _largest(samples, key):
    return max([r[key] for r in samples if r.get(key) is not None] or [None])


def _timer_mean_ms(p0, p1, base):
    """구간 합 증가분 / 개수 증가분 (밀리초) - 개수가 안 늘었으면 None"""
    c1, s1 = prom_sum(p1, base + "_count"), prom_sum(p1, base + "_sum")
    if c1 is None or s1 is None:
        return None
    n = c1 - (prom_sum(p0, base + "_count") or 0)
    return (s1 - (prom_sum(p0, base + "_sum") or 0)) / n * 1000 if n else None


def _seconds_to_ms(v):
    return None if v is None else v * 1000


def hikari_block(p0, p1, samples):
    """측정 구간의 풀 대기 수 최댓값, 획득·사용 시간(평균은 구간 시작·끝 스냅샷 차이, 최댓값은 5초 표본의 _max 게이지), 타임아웃 증가분
    pending 이 0 이 아니면 연결을 못 받고 기다린 요청이 있었다는 뜻 - active 가 최대여도 pending 0 이면 풀이 찬 것일 뿐 기다림은 없다"""
    timeouts = prom_sum(p1, "hikaricp_connections_timeout_total")
    return {
        "pending_peak": _largest(samples, "hikari_pending"),
        "acquire_ms": {"mean": _timer_mean_ms(p0, p1, "hikaricp_connections_acquire_seconds"),
                       "max": _seconds_to_ms(_largest(samples, "hikari_acquire_max_s"))},
        "usage_ms": {"mean": _timer_mean_ms(p0, p1, "hikaricp_connections_usage_seconds"),
                     "max": _seconds_to_ms(_largest(samples, "hikari_usage_max_s"))},
        "timeouts": None if timeouts is None else int(timeouts - (prom_sum(p0, "hikaricp_connections_timeout_total") or 0)),
    }


EMBED_TIMER = "gen_ai_client_operation_seconds"


def embedding_block(p0, p1, samples):
    """앱이 모의 서버(운영에서는 OpenAI)에 보낸 임베딩 호출 한 번의 시간 - Spring AI 클라이언트 타이머(gen_ai_operation_name=embedding), r129 전환 뒤의 Voyage 임베딩은 이 타이머가 없어 null (호출 수는 모의 서버 /stats 의 embeddings_by_input_type)
    RAG 검색 시간(rag.vector.search)은 이 호출 + pgvector 질의라서, 검색이 느려졌을 때 어느 쪽인지 가르는 값
    평균 = 구간 시작·끝 스냅샷의 합·개수 증가분, 최댓값 = 5초 표본의 _max 게이지 중 가장 큰 값, 오류 호출은 개수만 센다"""
    ok = {"gen_ai_operation_name": "embedding", "error": "none"}
    c0, c1 = prom_sum(p0, EMBED_TIMER + "_count", **ok), prom_sum(p1, EMBED_TIMER + "_count", **ok)
    s0, s1 = prom_sum(p0, EMBED_TIMER + "_sum", **ok), prom_sum(p1, EMBED_TIMER + "_sum", **ok)
    n = None if c1 is None else c1 - (c0 or 0)
    all1, all0 = prom_sum(p1, EMBED_TIMER + "_count", gen_ai_operation_name="embedding"), prom_sum(p0, EMBED_TIMER + "_count", gen_ai_operation_name="embedding")
    return {"count": None if n is None else int(n),
            "mean_ms": ((s1 - (s0 or 0)) / n * 1000) if n else None,
            "max_ms": _seconds_to_ms(_largest(samples, "embed_max_s")),
            "failed_count": None if all1 is None else int(all1 - (all0 or 0) - (n or 0))}


# 질의문 종류 - 위에서부터 처음 맞는 것 (vector_store 가 chat_messages 보다 앞, insert·update 가 from 보다 앞)
QUERY_CLASSES = (("vector", "vector_store"), ("chat_insert", "insert into chat_messages"),
                 ("chat_update", "update chat_messages"), ("chat_select", "from chat_messages"))
CLASS_NAMES = [c for c, _ in QUERY_CLASSES] + ["other"]
ACTIVITY_STATES = {"active": "active", "idle in transaction": "idle_in_transaction"}
ACTIVITY_SQL = ("SELECT state, COALESCE(EXTRACT(EPOCH FROM (now() - state_change)), 0), "
                r"regexp_replace(query, E'[\r\n]+', ' ', 'g') FROM pg_stat_activity "
                "WHERE datname = 'opener_s1' AND pid <> pg_backend_pid() AND state IN ('active', 'idle in transaction')")


def query_class(query):
    q = query.lower()
    return next((name for name, needle in QUERY_CLASSES if needle in q), "other")


def parse_activity(text):
    """psql -At 행 '상태|상태 경과 초|질의문' -> 표본 한 번의 상태 x 종류 개수와 트랜잭션 안 유휴 최장 시간"""
    out = {st: {c: 0 for c in CLASS_NAMES} for st in ACTIVITY_STATES.values()}
    idle_max = None
    for line in text.splitlines():
        parts = line.split("|", 2)
        if len(parts) != 3 or parts[0] not in ACTIVITY_STATES:
            continue
        out[ACTIVITY_STATES[parts[0]]][query_class(parts[2])] += 1
        if parts[0] == "idle in transaction":
            age = float(parts[1])
            idle_max = age if idle_max is None else max(idle_max, age)
    out["idle_in_tx_max_s"] = idle_max
    return out


def activity_block(samples):
    """표본 목록(읽기 실패는 None) -> 상태 x 종류 연결 수 최댓값, 표본 한 번의 상태별 합계 최댓값, 유휴 최장 시간"""
    ok = [x for x in samples if x]
    block = {"samples": len(ok), "samples_failed": len(samples) - len(ok), "peak": None, "peak_total": None, "idle_in_tx_max_s": None}
    if not ok:
        return block
    block["peak"] = {st: {c: max(x[st][c] for x in ok) for c in CLASS_NAMES} for st in ACTIVITY_STATES.values()}
    block["peak_total"] = {st: max(sum(x[st].values()) for x in ok) for st in ACTIVITY_STATES.values()}
    block["idle_in_tx_max_s"] = max([x["idle_in_tx_max_s"] for x in ok if x["idle_in_tx_max_s"] is not None] or [None])
    return block


BREAKERS = ("llm-chat", "llm-rag", "llm-summary", "redis-chat")
FAULT_TURN_TAIL_S = 60
CIRCUIT_BEFORE_S, CIRCUIT_AFTER_S = 10, 120
# 장애 구간 (hold 시작에서 시작까지 초, 길이 초) - 시험 실행은 짧게
FAULT_FULL, FAULT_TINY = (120, 60), (10, 10)


def circuit_sample(prom, t):
    """프로메테우스 표본 하나 -> 차단기 넷(llm 셋 + redis-chat)의 상태·허용 안 된 호출·실패 호출 (누적 값), 본문이 비었으면 None
    /actuator/circuitbreakers 는 로그인이 필요해(401) 같은 값을 열려 있는 prometheus 에서 읽는다"""
    if not prom:
        return None

    def series(metric, **labels):
        # prom_series 의 첫 인자가 name 이라 name 라벨을 키워드로 못 넘긴다
        return [(dict(lb), v) for (n, lb), v in prom.items() if n == metric and _match(lb, labels)]

    out = {}
    for name in BREAKERS:
        states = [d["state"] for d, v in series("resilience4j_circuitbreaker_state", name=name) if v == 1.0]
        if not states:
            continue
        out[name] = {"state": states[0].upper(),
                     "not_permitted": int(sum(v for _, v in series("resilience4j_circuitbreaker_not_permitted_calls_total", name=name))),
                     "failed": int(sum(v for _, v in series("resilience4j_circuitbreaker_calls_seconds_count", name=name, kind="failed")))}
    return {"t": t, "breakers": out}


def circuit_block(fault, samples):
    """장애 구간(에포크 ms)과 1초 표본 -> 차단기별 열림·닫힘 시간
    open_after_fault_s: 장애 시작 이후 처음 OPEN 인 표본까지 초 (안 열리면 null)
    closed_after_fault_end_s: 열린 적이 있을 때, 장애 끝 이후(그리고 열린 뒤) 처음 CLOSED 인 표본까지 초 (마지막 표본까지 안 닫히면 null)
    not_permitted · failed_calls: 첫 표본에서 마지막 표본까지의 증가분"""
    start_s, end_s = fault["start_ms"] / 1000, fault["end_ms"] / 1000
    result = {}
    for name in BREAKERS:
        rows = [(x["t"], x["breakers"][name]) for x in samples if x and name in x["breakers"]]
        opened = next((t for t, b in rows if t >= start_s and b["state"] == "OPEN"), None)
        closed = next((t for t, b in rows if opened is not None and t >= max(end_s, opened) and b["state"] == "CLOSED"), None)
        changes, last = [], None
        for t, b in rows:
            if b["state"] != last:
                changes.append([round(t - start_s, 3), b["state"]])
                last = b["state"]
        result[name] = {
            "open_after_fault_s": None if opened is None else opened - start_s,
            "closed_after_fault_end_s": None if closed is None else closed - end_s,
            "not_permitted": rows[-1][1]["not_permitted"] - rows[0][1]["not_permitted"] if rows else None,
            "failed_calls": rows[-1][1]["failed"] - rows[0][1]["failed"] if rows else None,
            "transitions": changes,
        }
    return result


def fault_turns(turns, start_ms, end_ms):
    """장애 시작부터 장애 끝 60초 뒤까지 시작한 턴 - 시도·성공·실패, 실패 종류별 수, 실패한 턴 시간(턴 시작 -> 오류), 성공한 턴의 첫 토큰 p95"""
    lo, hi = start_ms / 1000, end_ms / 1000 + FAULT_TURN_TAIL_S
    sel = [t for t in turns if lo <= t["t_start"] <= hi]
    ok = [t for t in sel if t["ok"]]
    failed = [t for t in sel if not t["ok"]]
    by_kind = {}
    for t in failed:
        by_kind[t["error"]] = by_kind.get(t["error"], 0) + 1
    fail_ms = [t["turn_ms"] for t in failed]
    return {"attempted": len(sel), "ok": len(ok), "failed": len(failed), "by_kind": by_kind,
            "failed_turn_ms": {"p50": percentile(fail_ms, 50), "p95": percentile(fail_ms, 95)},
            "ok_ttft_p95_ms": percentile([t["ttft_ms"] for t in ok if t["ttft_ms"] is not None], 95)}


def mock_delta(a, b):
    """장애 구간 시작·끝의 모의 서버 누적 요청 수 -> 구간 안 증가분, 한쪽을 못 읽었으면 None"""
    if not a or not b:
        return None
    return {k: b.get(k, 0) - a.get(k, 0) for k in ("chat_streams", "chat_nonstream", "embeddings")}


FAULT_KINDS = ("mock", "mock-embeddings", "master-stop", "cluster-pause")


MOCK_KINDS = ("mock", "mock-embeddings")


def mock_fault_body(kind, start_ms, end_ms):
    """모의 서버 /fault 본문 - mock 은 세 경로 모두, mock-embeddings 는 임베딩 경로만 (r130)"""
    body = {"start_ms": start_ms, "end_ms": end_ms}
    if kind == "mock-embeddings":
        body["routes"] = ["embeddings"]
    return body


def rag_fallback(p0, p1):
    """벡터 검색이 실패해 근거 없이 답한 횟수(경로별) - Micrometer 카운터는 처음 증가할 때 등록되므로 없으면 0"""
    return {path: int((prom_sum(p1, "rag_vector_search_fallback_total", path=path) or 0)
                      - (prom_sum(p0, "rag_vector_search_fallback_total", path=path) or 0)) for path in ("chat", "opener")}


def failed_turn_share(turns):
    """실패 턴 / 측정 구간에 끝난 턴 시도 수 - 옛 도구와 새 도구의 turns 줄이 같은 함수로 읽힌다 (ok·in_window 만 쓴다)"""
    window = [t for t in turns if t["in_window"]]
    failed = sum(1 for t in window if not t["ok"])
    return {"attempted": len(window), "failed": failed, "share": failed / len(window) if window else None}


def share_of_folders(folders):
    return {os.path.basename(os.path.normpath(d)): failed_turn_share(read_jsonl(d, "turns")) for d in folders}


def fault_block(fault, mock_stats, turns):
    """kind: mock = 모의 서버 500, master-stop = 마스터 하나 docker stop, cluster-pause = 여섯 노드 docker pause
    paused_ms · stopped_ms: 기록한 값이 있으면 그 값(실제 걸린 시간), 없으면 끝 - 시작"""
    failed = ((mock_stats or {}).get("fault") or {}).get("failed_by_route")
    kind = fault.get("kind", "mock")
    block = {"kind": kind, "start_ms": fault["start_ms"], "end_ms": fault["end_ms"], "failed_by_route": failed,
             "turns": fault_turns(turns, fault["start_ms"], fault["end_ms"])}
    span = fault["end_ms"] - fault["start_ms"]
    if kind == "cluster-pause":
        block["paused_ms"] = fault.get("paused_ms", span)
    if kind == "master-stop":
        block["stopped_ms"] = fault.get("stopped_ms", span)
        block["stopped_node"] = fault.get("stopped_node")
    if "mock_at_start" in fault:
        block["mock_delta"] = mock_delta(fault["mock_at_start"], fault.get("mock_at_end"))
    return block


def _int_or_float(v):
    return int(v) if v == int(v) else v


def streams_concurrent_max(turns, hold):
    """측정 구간(hold, 초) 안에서 같은 1초 칸에 스트림 중이던 턴 수의 최댓값
    스트림 구간 = 턴 시작 + 첫 토큰 시간 -> 턴 끝 (첫 토큰이 없는 턴은 스트림이 없다), 칸 s 는 구간이 [s, s+1) 과 겹칠 때 센다"""
    per = collections.Counter()
    for t in turns:
        if t.get("ttft_ms") is None:
            continue
        a, b = t["t_start"] + t["ttft_ms"] / 1000, t["t_end"]
        for sec in range(max(math.floor(hold[0]), math.floor(a)), min(math.ceil(hold[1]), math.ceil(b))):
            per[sec] += 1
    return max(per.values(), default=0)


def threads_block(samples, turns, hold):
    """플랫폼 스레드 수(jvm_threads_live_threads 는 가상 스레드를 세지 않는다)와 동시 스트림 수
    samples 는 측정 구간의 5초 표본, tomcat_busy 는 tomcat_threads_busy_threads (지표가 없으면 null)"""
    live = [r["jvm_threads_live"] for r in samples if r.get("jvm_threads_live") is not None]
    busy = [r["tomcat_busy"] for r in samples if r.get("tomcat_busy") is not None and r["tomcat_busy"] >= 0]  # 가상 스레드는 -1
    return {"platform_live_max": int(max(live)) if live else None,
            "platform_live_median": _int_or_float(statistics.median(live)) if live else None,
            "tomcat_busy_max": int(max(busy)) if busy else None,
            "streams_concurrent_max": streams_concurrent_max(turns, hold)}


def parse_cluster_nodes(text):
    """CLUSTER NODES 본문 -> 노드별 {id, name(호스트명, 없으면 ip:port), flags(집합), master_id(없으면 None), slots([[시작, 끝]])}"""
    nodes = []
    for line in text.splitlines():
        parts = line.split()
        if len(parts) < 8:
            continue
        addr = parts[1].split("@", 1)
        host = parts[1].split(",")
        slots = []
        for tok in parts[8:]:
            if tok.startswith("["):
                continue
            a, _, b = tok.partition("-")
            slots.append([int(a), int(b or a)])
        nodes.append({"id": parts[0], "name": host[1] if len(host) > 1 else addr[0], "flags": set(parts[2].split(",")),
                      "master_id": None if parts[3] == "-" else parts[3], "slots": slots})
    return nodes


def _owns(node, slot):
    return any(a <= slot <= b for a, b in node["slots"])


def failover_block(fault, samples):
    """멈춘 마스터의 슬롯이 복제본으로 넘어간 시간과 돌아온 뒤의 역할 (samples: {t, nodes} 1초 표본)
    promoted_after_s: 장애 시작 이후 처음, 멈춘 노드가 아닌 마스터(fail 표시 없음)가 멈춘 노드의 첫 슬롯을 가진 표본까지 초 (안 넘어가면 null)
    rejoined_as_replica: 장애 끝(docker start) 이후 멈춘 노드가 승격한 노드의 복제본(slave, fail 표시 없음)으로 보인 표본이 있는가"""
    start_s, end_s, name = fault["start_ms"] / 1000, fault["end_ms"] / 1000, fault.get("stopped_node")
    out = {"stopped_node": name, "promoted_after_s": None, "promoted_node": None, "rejoined_as_replica": False, "rejoined_after_s": None}
    before = [x for x in samples if x["t"] < start_s]
    stopped = next((n for n in before[-1]["nodes"] if n["name"] == name), None) if before else None
    if not stopped or not stopped["slots"]:
        return out
    probe = stopped["slots"][0][0]
    failing = {"fail", "fail?"}
    promoted = None
    for x in samples:
        if x["t"] < start_s:
            continue
        if promoted is None:
            promoted = next((n for n in x["nodes"] if n["id"] != stopped["id"] and "master" in n["flags"]
                             and not n["flags"] & failing and _owns(n, probe)), None)
            if promoted:
                out["promoted_after_s"], out["promoted_node"] = x["t"] - start_s, promoted["name"]
            continue
        if x["t"] >= end_s:
            me = next((n for n in x["nodes"] if n["id"] == stopped["id"]), None)
            if me and "slave" in me["flags"] and not me["flags"] & failing and me["master_id"] == promoted["id"]:
                out["rejoined_as_replica"], out["rejoined_after_s"] = True, x["t"] - end_s
                break
    return out


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


LAG_NOTE = ("행이 요약과 함께 저장된 경우도 updated_at - created_at 을 그대로 잰 값이다 "
            "(@LastModifiedDate 가 같은 INSERT 에서 created_at 보다 몇 ms 뒤에 찍힌다), 요약을 나중에 채우면 그 지연이 더해진다")


def save_summary_blocks(posts, rows, hold_ms=None):
    """posts: {question_result_id: 저장 요청 전송 시각(호스트 ms)}, rows: parse_rows 결과, hold_ms: (측정 구간 시작, 끝) 호스트 ms
    created_at 은 같은 호스트의 앱 JVM 이 쓰므로(@CreatedDate) DB 시계 오프셋은 빼지 않는다 - 오프셋은 따로 보고만 한다"""
    created = {r["qrid"]: r["created_ms"] for r in rows}
    durable = [created[q] - sent for q, sent in posts.items() if q in created]
    hold = None
    if hold_ms:
        in_hold = [created[q] - sent for q, sent in posts.items() if q in created and hold_ms[0] <= sent <= hold_ms[1]]
        hold = {**ms_stats(in_hold), "max": max(in_hold) if in_hold else None}
    lags = [max(0.0, (r["updated_ms"] or r["created_ms"]) - r["created_ms"]) for r in rows if not r["summary_null"]]
    missing = sum(1 for r in rows if r["summary_null"])
    not_durable = sum(1 for q in posts if q not in created)
    save = {"rows": len(rows), "posts": len(posts), "not_durable": not_durable, "durable_ms": ms_stats(durable),
            "durable_ms_hold": hold}
    summary = {"lag_ms": ms_stats(lags), "lag_note": LAG_NOTE, "missing_at_end": missing, "missing_incl_unsaved": missing + not_durable}
    return save, summary


def raw_blocks(out, sessions, window):
    """생성기 원자료(sessions)와 drain.json 에서 save·summary 블록을 만든다 - summarize 와 resummarize 가 같은 정의를 쓴다"""
    dr_path = os.path.join(out, "drain.json")
    dr = load_json(dr_path) if os.path.exists(dr_path) else None
    rows = parse_rows(dr["rows_raw"], dr["host_gmtoff_s"]) if dr else []
    # 같은 결과 id 로 저장 요청이 둘이면 먼저 보낸 것 - 행은 결과 id 당 하나뿐
    posts = {}
    for sess in sorted((x for x in sessions if x.get("save_code") == 200), key=lambda x: x["save_sent_ms"]):
        posts.setdefault(sess["question_result_id"], sess["save_sent_ms"])
    save_block, summary_block = save_summary_blocks(posts, rows, (window[0] * 1000, window[1] * 1000))
    save_block["clock_offset_ms"] = dr["clock_offset_ms"] if dr else None
    return dr, save_block, summary_block


def resummarize(out):
    """이미 만든 summary.json 의 save·summary 블록만 원자료로 다시 계산한다 - 앱·모의 서버가 꺼진 뒤라 나머지 값은 건드리지 않는다
    (옛 파일은 summary.v1.json 으로 남긴다)"""
    path = os.path.join(out, "summary.json")
    old = load_json(path)
    v1 = os.path.join(out, "summary.v1.json")
    if not os.path.exists(v1):
        dump_json(v1, old, ensure_ascii=False, indent=2)
    w = load_json(os.path.join(out, "window.json"))
    _, save_block, summary_block = raw_blocks(out, read_jsonl(out, "sessions"), (w["hold_start"], w["hold_end"]))
    summary_block["queue_published"] = old["summary"].get("queue_published")
    summary_block["queue_consumed"] = old["summary"].get("queue_consumed")
    old["save"], old["summary"] = save_block, summary_block
    dump_json(path, old, ensure_ascii=False, indent=2)


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


PROM_KEEP = ("chat_", "rag_vector", "gen_ai_client_operation_seconds", "jvm_threads", "process_cpu", "system_cpu",
             "hikaricp_connections", "resilience4j_circuitbreaker", "tomcat_threads", "jvm_gc_pause_seconds_count", "jvm_gc_pause_seconds_sum")


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
        with open(path) as f:
            return [int(x) for x in f.read().split()]
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


def wait_window(out):
    wpath = os.path.join(out, "window.json")
    for _ in range(600):
        if os.path.exists(wpath):
            break
        time.sleep(0.5)
    return load_json(wpath)


def post_json(url, obj, timeout=10):
    req = urllib.request.Request(url, json.dumps(obj).encode(), {"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return json.loads(r.read())


CHAT_CONTAINER = "r125s1-redis-chat-{}"


def docker_run(*args):
    return subprocess.run(["docker", *args], capture_output=True, text=True)


def cluster_nodes_now(ports, skip_name=None):
    """CLUSTER NODES 를 첫 번째로 답하는 노드에서 읽는다 - skip_name 노드(멈춘 노드)는 건너뛴다, 아무도 못 답하면 None"""
    for i, port in enumerate(ports):
        if skip_name and CHAT_CONTAINER.format(i + 1).endswith(skip_name):
            continue
        try:
            return parse_cluster_nodes(redis_cmd(port, "CLUSTER", "NODES"))
        except (OSError, RuntimeError, ValueError):
            continue
    return None


def heal_chat_cluster():
    """멈추거나 일시정지한 채팅 노드를 되돌린다 - 이미 돌고 있는 노드에는 무해하다"""
    names = [CHAT_CONTAINER.format(i) for i in range(1, 7)]
    docker_run("unpause", *names)
    docker_run("start", *names)


def redis_fault_thread(kind, sc, start_s, length, record):
    """장애 시작 시각까지 기다렸다가 docker stop(마스터 하나)·pause(여섯 노드)를 걸고 length 초 뒤 되돌린다
    record 에 실제 시작·끝(에포크 ms)과 모의 서버 누적 요청 수를 적는다, 끝 시각은 되돌리는 명령을 내리기 직전"""
    ports, mock = sc["ports"]["redis_chat"], f"http://{HOST}:{sc['ports']['mock']}"
    time.sleep(max(0.0, start_s - time.time()))
    record["mock_at_start"] = http_json(mock + "/stats")
    names = [CHAT_CONTAINER.format(i) for i in range(1, 7)]
    if kind == "master-stop":
        nodes = cluster_nodes_now(ports) or []
        masters = sorted(n["name"] for n in nodes if "master" in n["flags"] and "fail" not in n["flags"])
        if not masters:
            record["error"] = "CLUSTER NODES 에서 마스터를 못 찾았다"
            return
        record["stopped_node"] = masters[0]
        targets = [CHAT_CONTAINER.format(masters[0].rsplit("-", 1)[1])]
        down, up = ["stop"], ["start"]
    else:
        targets = names
        down, up = ["pause"], ["unpause"]
    t0 = time.time()
    res = docker_run(*down, *targets)
    record["down_rc"], record["down_done_ms"] = res.returncode, round(time.time() * 1000)
    time.sleep(max(0.0, t0 + length - time.time()))
    t1 = time.time()
    res = docker_run(*up, *targets)
    record["up_rc"], record["up_done_ms"] = res.returncode, round(time.time() * 1000)
    record["start_ms"], record["end_ms"] = round(t0 * 1000), round(t1 * 1000)
    record["mock_at_end"] = http_json(mock + "/stats")
    key = "stopped_ms" if kind == "master-stop" else "paused_ms"
    record[key] = record["end_ms"] - record["start_ms"]


def fault_run(out, sc, tiny, kind="mock"):
    """장애 구간(hold 시작 기준 FAULT_FULL, 시험 실행은 FAULT_TINY)을 걸고, 시작 10초 전부터 끝 120초 뒤까지 1초마다 차단기를 표집한다
    mock: 모의 서버에 구간을 건다 / master-stop · cluster-pause: 채팅 클러스터 컨테이너를 멈추거나 일시정지하고 CLUSTER NODES 도 1초마다 표집한다"""
    w = wait_window(out)
    delay, length = FAULT_TINY if tiny else FAULT_FULL
    start_s = w["hold_start"] + delay
    mock = f"http://{HOST}:{sc['ports']['mock']}"
    record, thread = {"kind": kind}, None
    if kind in MOCK_KINDS:
        record.update({"start_ms": round(start_s * 1000), "end_ms": round((start_s + length) * 1000)})
        body = mock_fault_body(kind, record["start_ms"], record["end_ms"])
        post_json(mock + "/fault", body)
        armed = (http_json(mock + "/stats") or {}).get("fault") or {}
        if armed.get("start_ms") != record["start_ms"] or armed.get("end_ms") != record["end_ms"] \
                or ("routes" in body and armed.get("routes") != body["routes"]):
            raise RuntimeError(f"장애 구간이 모의 서버에 안 걸렸다: {armed}")
    else:
        thread = threading.Thread(target=redis_fault_thread, args=(kind, sc, start_s, length, record), daemon=True)
        thread.start()
    stop = start_s + length + CIRCUIT_AFTER_S
    time.sleep(max(0.0, start_s - CIRCUIT_BEFORE_S - time.time()))
    samples, cluster, nxt = [], [], time.time()
    try:
        while time.time() < stop:
            now = time.time()
            row = circuit_sample(prom_from_snapshot(prom_snapshot(sc["ports"]["app"])), now)
            if row:
                samples.append(row)
            if kind == "master-stop":
                nodes = cluster_nodes_now(sc["ports"]["redis_chat"], record.get("stopped_node"))
                if nodes:
                    cluster.append({"t": now, "nodes": [{**n, "flags": sorted(n["flags"])} for n in nodes]})
            nxt += 1
            time.sleep(max(0.0, nxt - time.time()))
        if thread:
            thread.join(timeout=30)
    finally:
        if kind not in MOCK_KINDS:
            heal_chat_cluster()
    if kind not in MOCK_KINDS and "error" not in record and not {"start_ms", "end_ms"} <= set(record):
        raise RuntimeError(f"Redis 장애를 걸지 못했다: {record}")
    if "error" in record:
        raise RuntimeError(record["error"])
    dump_json(os.path.join(out, "circuit.json"), {"fault": record, "samples": samples, "cluster_samples": cluster, "tiny": tiny})


def watch(out, sc):
    w = wait_window(out)
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
        row["tomcat_busy"] = prom_max(prom, "tomcat_threads_busy_threads")
        row["process_cpu_usage"] = prom_value(prom, "process_cpu_usage")
        row["sse_active"] = prom_value(prom, "chat_sse_active_connections")
        row["hikari_active"] = prom_value(prom, "hikaricp_connections_active")
        row["hikari_pending"] = prom_sum(prom, "hikaricp_connections_pending")
        row["hikari_acquire_max_s"] = prom_max(prom, "hikaricp_connections_acquire_seconds_max")
        row["hikari_usage_max_s"] = prom_max(prom, "hikaricp_connections_usage_seconds_max")
        row["embed_max_s"] = prom_max(prom, EMBED_TIMER + "_max", gen_ai_operation_name="embedding", error="none")
        row["activity"] = read_activity()
        samples.append(row)
        nxt += SAMPLE_S
        if nxt >= w["hold_end"]:
            break
        time.sleep(max(0.0, nxt - time.time()))
    time.sleep(max(0.0, w["hold_end"] - time.time()))
    snap1 = snapshot(sc)
    dump_json(os.path.join(out, "collector.json"),
              {"window": w, "snap0": snap0, "snap1": snap1, "samples": samples, "write_cmds": write_cmds})


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


def read_activity():
    """pg_stat_activity 한 번 읽기 - 실패하면 None (표본 하나를 잃을 뿐 측정 구간 수집을 멈추지 않는다, 실패 수는 요약에 남는다)"""
    try:
        return parse_activity(psql_text(ACTIVITY_SQL))
    except (RuntimeError, ValueError, OSError):
        return None


def measure_clock_offset_ms():
    before = time.time()
    db = float(psql_text("SELECT EXTRACT(EPOCH FROM clock_timestamp())").strip())
    return clock_offset_ms(before, time.time(), db)


def drain(out, sc):
    """생성기가 끝난 직후부터 큐를 5초마다 최대 300초 표집하고, 끝나면 이번 회차의 chat_messages 행을 읽는다"""
    w = load_json(os.path.join(out, "window.json"))
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
    dump_json(os.path.join(out, "drain.json"),
              {"series": series, "end": rabbit_depths(sc), "clock_offset_ms": offset, "host_gmtoff_s": gmtoff, "rows_raw": text})


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
    dump_json(os.path.join(out, "idle.json"), res)


def host_info():
    def sh(*cmd):
        return subprocess.run(cmd, capture_output=True, text=True).stdout.strip()
    return {"os": platform.platform(), "cpus": os.cpu_count(), "mem_gb": round(int(sh("sysctl", "-n", "hw.memsize") or 0) / 2 ** 30, 1),
            "python": platform.python_version(), "docker_cpus": sh("docker", "info", "--format", "{{.NCPU}}"),
            "docker_mem_gb": round(int(sh("docker", "info", "--format", "{{.MemTotal}}") or 0) / 2 ** 30, 1)}


def read_jsonl(out, prefix):
    rows = []
    for p in sorted(glob.glob(os.path.join(out, prefix + "-*.jsonl"))):
        with open(p) as f:
            rows += [json.loads(l) for l in f if l.strip()]
    return rows


def summarize(out, code, commit, sc, app_env=None):
    col = load_json(os.path.join(out, "collector.json"))
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
    idle = load_json(idle_path) if os.path.exists(idle_path) else {}
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
    dr, save_block, summary_block = raw_blocks(out, sessions, window)
    summary_block["queue_published"] = (sq1.get("stats", {}).get("publish", 0)) - (sq0.get("stats", {}).get("publish", 0)) if sq1 else None
    summary_block["queue_consumed"] = (sq1.get("stats", {}).get("ack", 0)) - (sq0.get("stats", {}).get("ack", 0)) if sq1 else None
    depth_end = depth_pair({k: v["messages"] for k, v in s1["rabbit"].items()})
    if "depths_at_hold_end" in s1:
        depth_end = s1["depths_at_hold_end"]
    final_rabbit = http_json(f"http://{HOST}:{sc['ports']['rabbit_mgmt']}/api/queues", "guest:guest") or []
    final_mock = http_json(f"http://{HOST}:{sc['ports']['mock']}/stats")
    fault = circuit = failover = None
    circ_path = os.path.join(out, "circuit.json")
    if os.path.exists(circ_path):
        circ = load_json(circ_path)
        fault, circuit = fault_block(circ["fault"], final_mock, turns), circuit_block(circ["fault"], circ["samples"])
        if circ["fault"].get("kind") == "master-stop":
            cluster_samples = [{"t": x["t"], "nodes": [{**n, "flags": set(n["flags"])} for n in x["nodes"]]} for x in circ.get("cluster_samples", [])]
            failover = failover_block(circ["fault"], cluster_samples)
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
            "rag_fallback": rag_fallback(p0, p1),
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
        "app_embedding_call_ms": embedding_block(p0, p1, samples),
        "db": {"hikari": hikari_block(p0, p1, samples), "activity": activity_block([r.get("activity") for r in samples])},
        "fault": fault, "circuit": circuit, "failover": failover,
        "threads": threads_block(samples, turns, window),
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
        "mock_cpu_mean_pct": (lambda v: sum(v) / len(v) if v else None)([sum(r["mock_cpu_pct"]) for r in samples if r["mock_cpu_pct"]]),
        "host": host_info(),
        "sessions_completed_total": sum(1 for s in sessions if s.get("ok")),
        "attempts_in_window": ts["attempts_in_window"], "window": {"hold_start": w["hold_start"], "hold_end": w["hold_end"],
                                                                    "snapshot_span_s": dur},
        "errors_rate_def": "측정 구간 안 오류 수 / 측정 구간에 끝난 턴 시도 수(성공·실패 모두)",
        "top_samples": [r["top"] for r in samples if r.get("top")][:: max(1, len(samples) // 12)],
    }
    dump_json(os.path.join(out, "summary.json"), summary, ensure_ascii=False, indent=2)
    return summary


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("mode", choices=["watch", "fault", "drain", "summarize", "resummarize", "idle", "share"])
    ap.add_argument("folders", nargs="*", help="share: 실행 폴더 목록")
    ap.add_argument("--share-out", help="share: 폴더 이름별 실패 턴 비율을 쓸 JSON 파일")
    ap.add_argument("--seconds", type=float, default=20)
    ap.add_argument("--out")
    ap.add_argument("--scenario", default=os.path.join(HERE, "s1", "scenario.json"))
    ap.add_argument("--code")
    ap.add_argument("--commit")
    ap.add_argument("--app-env", default="")
    ap.add_argument("--tiny", action="store_true")
    ap.add_argument("--kind", choices=FAULT_KINDS, default="mock")
    args = ap.parse_args()
    if args.mode == "share":
        dump_json(args.share_out, share_of_folders(args.folders), indent=2)
        return
    if not args.out:
        ap.error("--out 이 필요하다")
    sc = load_json(args.scenario)
    if args.mode == "watch":
        watch(args.out, sc)
    elif args.mode == "fault":
        fault_run(args.out, sc, args.tiny, args.kind)
    elif args.mode == "resummarize":
        resummarize(args.out)
    elif args.mode == "drain":
        drain(args.out, sc)
    elif args.mode == "idle":
        idle_baseline(args.out, sc, args.seconds)
    else:
        summarize(args.out, args.code, args.commit, sc, dict(kv.split("=", 1) for kv in args.app_env.split() if "=" in kv))


if __name__ == "__main__":
    main()
