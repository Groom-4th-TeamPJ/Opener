#!/usr/bin/env python3
"""S1 부하 생성기 - 가상 사용자(VU) 1,000명이 채팅 세션을 동시에 유지한다 (asyncio, 표준 라이브러리만 사용)

VU 하나의 흐름: 로그인 1회 -> 세션마다 SSE 연결, 턴 N회(생각 시간 사이에), save-message, disconnect -> 다음 세션
여러 프로세스가 VU 를 나눠 맡고, 조정 프로세스가 시작 시각·측정 구간(window.json)을 정한다
사용: s1_load.py --users users.json --out DIR [--tiny]
"""
import argparse
import asyncio
import collections
import json
import os
import random
import subprocess
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
QUESTIONS = [
    "미적분에서 극한의 개념을 쉽게 설명해줘",
    "이차함수의 최댓값을 구하는 방법을 알려줘",
    "확률과 통계에서 조건부확률이 뭔지 예시로 설명해줘",
    "등차수열의 합 공식을 유도해줘",
    "삼각함수의 덧셈정리를 어떻게 외우면 좋을까",
    "벡터의 내적이 무슨 뜻인지 그림 없이 설명해줘",
    "로그의 성질을 문제 풀이에 쓰는 요령을 알려줘",
    "정적분으로 넓이를 구하는 과정을 단계별로 보여줘",
    "수열의 극한에서 샌드위치 정리를 쓰는 때가 언제야",
    "이 문제에서 내가 틀린 이유가 뭔지 짚어줘",
]


# ---------- SSE ----------
class SseParser:
    """읽기 단위와 무관하게 빈 줄로 끝나는 프레임만 이벤트로 돌려준다"""

    def __init__(self):
        self.buf = b""

    def feed(self, data):
        self.buf = (self.buf + data).replace(b"\r\n", b"\n")
        events = []
        while True:
            cut = self.buf.find(b"\n\n")
            if cut < 0:
                return events
            frame, self.buf = self.buf[:cut], self.buf[cut + 2:]
            event, datas = None, []
            for line in frame.decode("utf-8", "replace").split("\n"):
                if not line or line.startswith(":"):
                    continue
                field, _, value = line.partition(":")
                value = value[1:] if value.startswith(" ") else value
                if field == "event":
                    event = value
                elif field == "data":
                    datas.append(value)
            if event is not None or datas:
                events.append((event or "message", "\n".join(datas)))


# ---------- 통계 보조 ----------
def percentile(values, p):
    """가장 가까운 순위 방식 - 값 N 개 중 ceil(p/100*N) 번째"""
    if not values:
        return None
    s = sorted(values)
    rank = -(-len(s) * p // 100)
    return s[max(1, int(rank)) - 1]


def think_time(rng, think_s):
    return rng.uniform(think_s[0], think_s[1])


def in_window(t_start, t_end, window):
    return window[0] <= t_start and t_end <= window[1]


def question_text(session, turn, template, rng):
    return template.format(session=session, turn=turn) + rng.choice(QUESTIONS)


# ---------- 최소 HTTP 클라이언트 ----------
async def read_head(reader):
    head = await reader.readuntil(b"\r\n\r\n")
    lines = head.decode("latin-1").split("\r\n")
    status = int(lines[0].split(" ", 2)[1])
    headers = [(k.strip().lower(), v.strip()) for k, v in (l.split(":", 1) for l in lines[1:] if ":" in l)]
    return status, headers


def header(headers, name):
    return next((v for k, v in headers if k == name), None)


async def iter_chunks(reader):
    while True:
        size = int((await reader.readuntil(b"\r\n")).split(b";")[0], 16)
        if size == 0:
            await reader.readuntil(b"\r\n")
            return
        yield (await reader.readexactly(size + 2))[:-2]


def build_request(method, host, port, path, cookie, body=None, accept=None, close=True):
    lines = [f"{method} {path} HTTP/1.1", f"Host: {host}:{port}"]
    if cookie:
        lines.append(f"Cookie: {cookie}")
    if accept:
        lines.append(f"Accept: {accept}")
    lines.append("Connection: close" if close else "Connection: keep-alive")
    data = b""
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        lines += ["Content-Type: application/json", f"Content-Length: {len(data)}"]
    elif method == "POST":
        lines.append("Content-Length: 0")
    return ("\r\n".join(lines) + "\r\n\r\n").encode("latin-1") + data


async def http_request(host, port, method, path, cookie=None, body=None, timeout=30):
    """요청마다 새 연결 (Connection: close) - 유휴 keep-alive 연결이 서버에서 닫힌 경우의 재시도를 없앤다"""
    reader, writer = await asyncio.wait_for(asyncio.open_connection(host, port), timeout)
    try:
        writer.write(build_request(method, host, port, path, cookie, body))
        await writer.drain()
        status, headers = await asyncio.wait_for(read_head(reader), timeout)
        if header(headers, "transfer-encoding") == "chunked":
            data = b"".join([c async for c in iter_chunks(reader)])
        elif header(headers, "content-length") is not None:
            data = await reader.readexactly(int(header(headers, "content-length")))
        else:
            data = await reader.read()
        return status, headers, data
    finally:
        writer.close()


def status_kind(status):
    return "http_5xx" if status >= 500 else "http_4xx"


# ---------- 기록 ----------
class Recorder:
    def __init__(self):
        self.turns = []
        self.sessions = []
        self.errors = collections.Counter()
        self.error_events = []  # (시각, 종류, vu) - 측정 구간 오류율 계산용

    def error(self, kind, vu):
        self.errors[kind] += 1
        self.error_events.append((time.time(), kind, vu))


class TurnState:
    def __init__(self):
        self.connected = asyncio.Event()
        self.done = asyncio.Event()
        self.want = None  # 첫 토큰이 담아야 할 답변 표식
        self.t_first = self.t_end = self.error = None
        self.error_detail = None  # SSE error 이벤트의 본문 앞부분 - 사용자가 받는 오류 문구를 남긴다

    def begin(self, want):
        self.want, self.t_first, self.t_end, self.error, self.error_detail = want, None, None, None, None
        self.done = asyncio.Event()


async def pump(reader, st):
    """SSE 연결을 읽어 현재 턴 상태를 갱신 - 이벤트마다 JSON 을 풀지 않고 첫 토큰의 표식만 문자열 검색"""
    parser = SseParser()
    try:
        async for piece in iter_chunks(reader):
            for event, data in parser.feed(piece):
                if event == "message":
                    if st.want is not None and st.t_first is None:
                        st.t_first = time.monotonic()
                        if st.want not in data:
                            st.error = "marker_missing"
                            st.t_end = st.t_first
                            st.done.set()
                elif event == "complete":
                    if st.want is not None and st.t_end is None:
                        st.t_end = time.monotonic()
                        st.done.set()
                elif event == "error":
                    if st.want is not None and st.t_end is None:
                        st.t_end, st.error, st.error_detail = time.monotonic(), "sse_error_event", data[:300]
                        st.done.set()
                elif event == "connected":
                    st.connected.set()
    except (asyncio.IncompleteReadError, ConnectionError, ValueError):
        pass
    if st.want is not None and st.t_end is None:
        st.t_end, st.error = time.monotonic(), "sse_closed"
        st.done.set()


def cookie_of(headers):
    return "; ".join(v.split(";")[0] for k, v in headers if k == "set-cookie")


# ---------- VU ----------
async def run_turn(spec, rec, st, cookie, sess, turn, rng):
    sid = sess["session_id"]
    text = question_text(sid, turn, spec["markers"]["question"], rng)
    st.begin(spec["markers"]["answer"].format(session=sid, turn=turn).rstrip())
    t_start, m0 = time.time(), time.monotonic()
    err = None
    try:
        status, _, _ = await http_request(
            spec["host"], spec["port"], "POST", spec["prefix"] + "/chat/message", cookie,
            {"sessionId": sid, "questionId": sess.get("question_id", 1), "message": text})
        if status != 200:
            err = status_kind(status)
        else:
            try:
                await asyncio.wait_for(st.done.wait(), spec["turn_timeout_s"])
                err = st.error
            except asyncio.TimeoutError:
                err = "turn_timeout"
    except (OSError, asyncio.TimeoutError, asyncio.IncompleteReadError):
        err = "conn_error"
    t_end_mono = st.t_end if st.t_end is not None else time.monotonic()
    turn_ms = (t_end_mono - m0) * 1000
    st.want = None
    rec.turns.append({
        "vu": spec["vu"], "session": sid, "turn": turn, "t_start": t_start, "t_end": t_start + turn_ms / 1000,
        "ttft_ms": (st.t_first - m0) * 1000 if st.t_first is not None else None,
        "turn_ms": turn_ms, "ok": err is None, "error": err, "error_detail": st.error_detail if err == "sse_error_event" else None,
        "in_window": in_window(t_start, t_start + turn_ms / 1000, spec["window"]),
    })
    if err:
        rec.error(err, spec["vu"])
    return err is None


async def run_session(spec, rec, cookie, sess, rng):
    host, port, prefix, sid = spec["host"], spec["port"], spec["prefix"], sess["session_id"]
    t0 = time.monotonic()
    st = TurnState()
    try:
        reader, writer = await asyncio.wait_for(asyncio.open_connection(host, port), 30)
        writer.write(build_request("GET", host, port, f"{prefix}/chat/connect?sessionId={sid}", cookie,
                                   accept="text/event-stream", close=False))
        await writer.drain()
        status, headers = await asyncio.wait_for(read_head(reader), 30)
    except (OSError, asyncio.TimeoutError, asyncio.IncompleteReadError):
        rec.error("conn_error", spec["vu"])
        return
    if status != 200:
        rec.error(status_kind(status), spec["vu"])
        writer.close()
        return
    task = asyncio.create_task(pump(reader, st))
    info = {"vu": spec["vu"], "session": sid, "turns": 0, "ok": False}
    try:
        try:
            await asyncio.wait_for(st.connected.wait(), spec["turn_timeout_s"])
        except asyncio.TimeoutError:
            rec.error("connect_failed", spec["vu"])
            return
        info["connect_ms"] = (time.monotonic() - t0) * 1000
        ok = True
        for turn in range(1, spec["turns_per_session"] + 1):
            if spec["stop_at"] and time.time() >= spec["stop_at"]:
                break
            if turn > 1:
                await asyncio.sleep(think_time(rng, spec["think_s"]))
            ok = await run_turn(spec, rec, st, cookie, sess, turn, rng)
            info["turns"] += 1
            if not ok:
                break
        if info["turns"]:
            m = time.monotonic()
            # 저장 요청 전송 시각(호스트 시계)과 결과 id - 수집기가 chat_messages 행의 생성 시각과 이어 붙인다
            info["save_sent_ms"], info["question_result_id"] = time.time() * 1000, sess["question_result_id"]
            code, _, _ = await http_request(host, port, "POST", prefix + "/chat/save-message", cookie,
                                            {"sessionId": sid, "questionResultId": sess["question_result_id"]})
            info["save_ms"], info["save_code"] = (time.monotonic() - m) * 1000, code
            if code != 200:
                rec.error(status_kind(code), spec["vu"])
        m = time.monotonic()
        code, _, _ = await http_request(host, port, "POST", f"{prefix}/chat/disconnect?sessionId={sid}", cookie)
        info["disconnect_ms"] = (time.monotonic() - m) * 1000
        if code != 200:
            rec.error(status_kind(code), spec["vu"])
        info["ok"] = ok
    except (OSError, asyncio.TimeoutError, asyncio.IncompleteReadError):
        rec.error("conn_error", spec["vu"])
    finally:
        task.cancel()
        writer.close()
        info["t_end"] = time.time()
        rec.sessions.append(info)


async def run_vu(spec, rec):
    rng = random.Random(spec["seed"])
    if spec.get("start_at"):
        await asyncio.sleep(max(0.0, spec["start_at"] - time.time()))
    try:
        status, headers, _ = await http_request(
            spec["host"], spec["port"], "POST", spec["prefix"] + "/auth/form-login", None,
            {"email": spec["email"], "password": spec["password"]})
    except (OSError, asyncio.TimeoutError, asyncio.IncompleteReadError):
        rec.error("login_failed", spec["vu"])
        return
    if status != 200:
        rec.error("login_failed", spec["vu"])
        return
    cookie = cookie_of(headers)
    done = 0
    while spec["max_sessions"] is None or done < spec["max_sessions"]:
        if spec["stop_at"] and time.time() >= spec["stop_at"]:
            break
        sess = spec["sessions"][done % len(spec["sessions"])]
        await run_session(spec, rec, cookie, sess, rng)
        done += 1


# ---------- 프로세스 ----------
def load_scenario(path, tiny):
    with open(path, encoding="utf-8") as f:
        sc = json.load(f)
    if tiny:
        sc.update(sc["tiny"])
    sc.setdefault("max_sessions_per_vu", None)
    return sc


def vu_specs(sc, users, window, host, port, shard, shards):
    n = sc["vus"]
    specs = []
    for i in range(shard, n, shards):
        u = users[i]
        frac = i / n
        specs.append({
            "vu": i, "host": host, "port": port, "prefix": "/api",
            "email": u["email"], "password": u["password"], "sessions": u["sessions"],
            "turns_per_session": sc["turns_per_session"], "think_s": sc["think_s"],
            "turn_timeout_s": sc["turn_timeout_s"], "markers": sc["markers"],
            "window": (window["hold_start"], window["hold_end"]),
            "start_at": window["t0"] + sc["ramp_up_s"] * frac,
            "stop_at": window["hold_end"] + sc["ramp_down_s"] * frac,
            "max_sessions": sc["max_sessions_per_vu"], "seed": 1000 + i,
        })
    return specs


def dump_jsonl(path, rows):
    with open(path, "w", encoding="utf-8") as f:
        for r in rows:
            f.write(json.dumps(r, ensure_ascii=False) + "\n")


async def worker_main(specs, out, shard):
    rec = Recorder()
    await asyncio.gather(*(run_vu(s, rec) for s in specs))
    dump_jsonl(os.path.join(out, f"turns-{shard}.jsonl"), rec.turns)
    dump_jsonl(os.path.join(out, f"sessions-{shard}.jsonl"), rec.sessions)
    dump_jsonl(os.path.join(out, f"errors-{shard}.jsonl"),
               [{"t": t, "kind": k, "vu": v} for t, k, v in rec.error_events])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--scenario", default=os.path.join(HERE, "s1", "scenario.json"))
    ap.add_argument("--users", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--tiny", action="store_true")
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--worker", type=int)
    ap.add_argument("--of", type=int)
    ap.add_argument("--t0", type=float)
    args = ap.parse_args()
    sc = load_scenario(args.scenario, args.tiny)
    port = sc["ports"]["app"]
    with open(args.users, encoding="utf-8") as f:
        users = json.load(f)
    if len(users) < sc["vus"]:
        sys.exit(f"사용자 {len(users)}명 < vus {sc['vus']}")
    t0 = args.t0 or time.time() + 5
    window = {"t0": t0, "hold_start": t0 + sc["ramp_up_s"], "hold_end": t0 + sc["ramp_up_s"] + sc["hold_s"]}
    if args.worker is not None:
        asyncio.run(worker_main(vu_specs(sc, users, window, args.host, port, args.worker, args.of), args.out, args.worker))
        return
    procs = sc.get("loadgen_procs", 1)
    os.makedirs(args.out, exist_ok=True)
    window.update({"vus": sc["vus"], "hold_s": sc["hold_s"], "ramp_up_s": sc["ramp_up_s"],
                   "ramp_down_s": sc["ramp_down_s"], "procs": procs, "end": window["hold_end"] + sc["ramp_down_s"]})
    with open(os.path.join(args.out, "window.json"), "w") as f:
        json.dump(window, f)
    cmd = [sys.executable, os.path.abspath(__file__), "--scenario", args.scenario, "--users", args.users,
           "--out", args.out, "--host", args.host, "--of", str(procs), "--t0", repr(t0)] + (["--tiny"] if args.tiny else [])
    children = [subprocess.Popen(cmd + ["--worker", str(k)]) for k in range(procs)]
    with open(os.path.join(args.out, "gen-pids.txt"), "w") as f:
        f.write("\n".join(str(c.pid) for c in children))
    codes = [c.wait() for c in children]
    sys.exit(max(codes))


if __name__ == "__main__":
    main()
