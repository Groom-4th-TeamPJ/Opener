#!/usr/bin/env python3
"""모의 OpenAI 서버 - S1 부하 시나리오의 외부 LLM 대역 (표준 라이브러리만 사용)

POST /v1/chat/completions (스트림·비스트림), POST /v1/embeddings, GET /stats, POST /stats/reset
지연·토큰 속도는 s1/scenario.json 의 llm 값을 따른다
"""
import argparse
import asyncio
import hashlib
import json
import logging
import math
import os
import random
import re
import struct
import threading
import time

log = logging.getLogger("mock_openai")

EMBED_DIM = 1536
# 모든 벡터에 공통 성분을 섞어 서로 무관한 글끼리도 코사인 유사도가 약 0.3 이 되게 한다
# 실제 임베딩도 무관한 글끼리 0.2~0.4 라서, 0 에 가까우면 RAG 검색(임계값 0.2)이 0건이 되어 운영 경로와 달라진다
COMMON_WEIGHT = math.sqrt(0.3)
WORD = re.compile(r"\w+", re.UNICODE)
# 한글 음절 2자 = 토큰 1개 (본문 바이트 약 6)
SYLLABLES = "가나다라마바사아자차카타파하거너더러머버서어저처커터퍼허고노도로모보소오조초코토포호"

MARKER_Q = re.compile(r"^\[q s=(\d+) t=(\d+)\] ")
MARKER_A = re.compile(r"^\[a s=(\d+) t=(\d+)\] ")


def _unit(vec):
    norm = math.sqrt(sum(x * x for x in vec)) or 1.0
    return [x / norm for x in vec]


_COMMON = _unit([math.sin(i * 0.7371 + 1.0) for i in range(EMBED_DIM)])


def embedding_for(text):
    """같은 글 -> 같은 벡터, 단어를 공유하면 가까운 벡터 (단어별 해시로 3개 차원에 ±1)"""
    bow = [0.0] * EMBED_DIM
    for word in WORD.findall(text.lower()) or [text]:
        digest = hashlib.sha256(word.encode("utf-8")).digest()
        for k in range(3):
            idx = struct.unpack_from(">H", digest, k * 2)[0] % EMBED_DIM
            bow[idx] += 1.0 if digest[6 + k] & 1 else -1.0
    bow = _unit(bow)
    w = math.sqrt(1.0 - COMMON_WEIGHT ** 2)
    return [COMMON_WEIGHT * c + w * b for c, b in zip(_COMMON, bow)]


def _text_of(content):
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        return "".join(p.get("text", "") for p in content if isinstance(p, dict))
    return ""


def check_prompt(messages):
    """위반 사유를 돌려주고, 완전한 프롬프트면 None

    턴 n 의 질문이면 사용자 표식 1..n, 답변 표식 1..n-1 이 각 1번씩 순서대로 있고 마지막이 턴 n 질문이어야 한다
    """
    convo = [(m.get("role"), _text_of(m.get("content"))) for m in messages if m.get("role") != "system"]
    users = [(i, MARKER_Q.match(t)) for i, (r, t) in enumerate(convo) if r == "user"]
    if not users or users[-1][1] is None:
        return "no_marker"
    session, n = users[-1][1].group(1), int(users[-1][1].group(2))
    seq = []  # (역할, 턴)
    for role, text in convo:
        m = (MARKER_Q if role == "user" else MARKER_A).match(text)
        if m is None:
            return "no_marker"
        if m.group(1) != session:
            return "foreign_session"
        seq.append(("q" if role == "user" else "a", int(m.group(2))))
    if seq.count(("q", n)) > 1:
        return "current_question_twice"
    for k in range(1, n):
        c = seq.count(("a", k))
        if c != 1:
            return "answer_missing" if c == 0 else "answer_duplicated"
        c = seq.count(("q", k))
        if c != 1:
            return "question_missing" if c == 0 else "question_duplicated"
    expected = [x for k in range(1, n) for x in (("q", k), ("a", k))] + [("q", n)]
    if seq[-1] != ("q", n):
        return "current_question_not_last"
    if seq != expected:
        return "order_violation"
    return None


class Stats:
    def __init__(self):
        self.reset()

    def reset(self):
        self.chat_streams = 0
        self.chat_nonstream = 0
        self.embeddings = 0
        self.prompt_checked = 0
        self.prompt_missing = 0
        self.by_reason = {}
        self.active_streams = 0
        self.active_streams_peak = 0

    def as_dict(self):
        return {
            "chat_streams": self.chat_streams,
            "chat_nonstream": self.chat_nonstream,
            "embeddings": self.embeddings,
            "prompt_checked": self.prompt_checked,
            "prompt_missing": self.prompt_missing,
            "prompt_missing_by_reason": dict(self.by_reason),
            "active_streams_peak": self.active_streams_peak,
        }


def _chunk_json(model, cid, delta, finish=None):
    return json.dumps({
        "id": cid, "object": "chat.completion.chunk", "created": int(time.time()), "model": model,
        "choices": [{"index": 0, "delta": delta, "finish_reason": finish}],
    }, ensure_ascii=False)


class MockServer:
    # shortcut: 단일 이벤트 루프 한 개라 초당 청크 약 5만 개가 상한, 넘으면 SO_REUSEPORT 로 프로세스를 늘린다
    def __init__(self, scenario, host="127.0.0.1", port=18090):
        self.llm = scenario["llm"]
        self.host, self.port = host, port
        self.stats = Stats()
        self._loop = None
        self._server = None
        self._thread = None
        self._ready = threading.Event()

    # ---- 수명 ----
    def start(self):
        self._thread = threading.Thread(target=self._run, daemon=True)
        self._thread.start()
        self._ready.wait(10)

    def stop(self):
        if self._loop:
            self._loop.call_soon_threadsafe(self._loop.stop)
            self._thread.join(5)

    def _run(self):
        self._loop = asyncio.new_event_loop()
        asyncio.set_event_loop(self._loop)
        self._server = self._loop.run_until_complete(
            asyncio.start_server(self._handle, self.host, self.port, backlog=4096))
        self.port = self._server.sockets[0].getsockname()[1]
        self._ready.set()
        try:
            self._loop.run_forever()
        finally:
            self._server.close()
            pending = asyncio.all_tasks(self._loop)
            for t in pending:
                t.cancel()
            self._loop.run_until_complete(asyncio.gather(*pending, return_exceptions=True))
            self._loop.close()

    def serve_forever(self):
        self._run()

    # ---- HTTP ----
    async def _handle(self, reader, writer):
        try:
            while True:
                head = await reader.readuntil(b"\r\n\r\n")
                lines = head.decode("latin-1").split("\r\n")
                method, path, _ = lines[0].split(" ", 2)
                headers = {k.lower(): v.strip() for k, v in (l.split(":", 1) for l in lines[1:] if ":" in l)}
                if headers.get("expect", "").lower() == "100-continue":
                    writer.write(b"HTTP/1.1 100 Continue\r\n\r\n")
                if headers.get("transfer-encoding", "").lower() == "chunked":
                    body = b""  # JDK HttpClient 가 본문 길이를 모를 때 쓰는 방식
                    while True:
                        size = int((await reader.readuntil(b"\r\n")).split(b";")[0], 16)
                        body += (await reader.readexactly(size + 2))[:-2]
                        if size == 0:
                            break
                else:
                    body = await reader.readexactly(int(headers.get("content-length", "0")))
                keep = headers.get("connection", "keep-alive").lower() != "close"
                await self._route(method, path.split("?")[0], body, writer)
                if not keep:
                    break
        except (asyncio.IncompleteReadError, ConnectionError, asyncio.LimitOverrunError):
            pass
        except Exception:
            log.exception("요청 처리 실패")
        finally:
            writer.close()

    async def _route(self, method, path, body, writer):
        if method == "GET" and path == "/stats":
            return self._json(writer, 200, self.stats.as_dict())
        if method == "POST" and path == "/stats/reset":
            self.stats.reset()
            return self._json(writer, 200, {"ok": True})
        payload = json.loads(body or b"{}")
        if method == "POST" and path == "/v1/embeddings":
            return await self._embeddings(writer, payload)
        if method == "POST" and path == "/v1/chat/completions":
            if payload.get("stream"):
                return await self._chat_stream(writer, payload)
            return await self._chat_once(writer, payload)
        self._json(writer, 404, {"error": {"message": f"no route {method} {path}"}})

    @staticmethod
    def _json(writer, status, obj):
        data = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        writer.write(f"HTTP/1.1 {status} X\r\nContent-Type: application/json\r\nContent-Length: {len(data)}\r\n\r\n"
                     .encode("latin-1") + data)

    # ---- 임베딩 ----
    async def _embeddings(self, writer, payload):
        self.stats.embeddings += 1
        await asyncio.sleep(self.llm["embedding_ms"] / 1000)
        inputs = payload.get("input", "")
        inputs = [inputs] if isinstance(inputs, str) else inputs
        data = [{"object": "embedding", "index": i, "embedding": embedding_for(t if isinstance(t, str) else str(t))}
                for i, t in enumerate(inputs)]
        self._json(writer, 200, {"object": "list", "data": data, "model": payload.get("model", "mock"),
                                 "usage": {"prompt_tokens": 8 * len(inputs), "total_tokens": 8 * len(inputs)}})

    # ---- 채팅 ----
    def _check(self, messages):
        self.stats.prompt_checked += 1
        reason = check_prompt(messages)
        if reason:
            self.stats.prompt_missing += 1
            self.stats.by_reason[reason] = self.stats.by_reason.get(reason, 0) + 1
            tail = [(m.get("role"), _text_of(m.get("content"))[:24]) for m in messages if m.get("role") != "system"]
            log.warning("프롬프트 위반 reason=%s 메시지=%s", reason, tail)

    def _answer_head(self, messages):
        for m in reversed(messages):
            if m.get("role") == "user":
                mm = MARKER_Q.match(_text_of(m.get("content")))
                if mm:
                    return f"[a s={mm.group(1)} t={mm.group(2)}] "
                break
        return ""

    def _tokens(self, count, head):
        n = self.llm["chars_per_token"]
        rnd = random.Random(head)
        toks = ["".join(rnd.choice(SYLLABLES) for _ in range(n)) for _ in range(count)]
        toks[0] = head + toks[0]  # 표식은 첫 토큰에 붙여 청크 수를 answer_tokens 로 유지
        return toks

    async def _chat_stream(self, writer, payload):
        st = self.stats
        st.chat_streams += 1
        st.active_streams += 1
        st.active_streams_peak = max(st.active_streams_peak, st.active_streams)
        try:
            messages = payload.get("messages", [])
            self._check(messages)
            model, cid = payload.get("model", "mock"), f"chatcmpl-{random.getrandbits(48):x}"
            lo, hi = self.llm["first_token_ms"]
            toks = self._tokens(self.llm["answer_tokens"], self._answer_head(messages))
            writer.write(b"HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nCache-Control: no-cache\r\n"
                         b"Transfer-Encoding: chunked\r\n\r\n")
            await asyncio.sleep(random.uniform(lo, hi) / 1000)
            interval = 1.0 / self.llm["tokens_per_s"]
            t0 = time.monotonic()
            self._send(writer, _chunk_json(model, cid, {"role": "assistant", "content": ""}))
            for i, tok in enumerate(toks):
                # 누적 오차를 막으려고 시작 시각 기준 절대 시각에 맞춘다
                wait = t0 + i * interval - time.monotonic()
                if wait > 0:
                    await asyncio.sleep(wait)
                self._send(writer, _chunk_json(model, cid, {"content": tok}))
                if writer.transport.is_closing():
                    return
                if i % 8 == 7:
                    await writer.drain()
            self._send(writer, _chunk_json(model, cid, {}, "stop"))
            self._send(writer, "[DONE]")
            writer.write(b"0\r\n\r\n")
            await writer.drain()
        finally:
            st.active_streams -= 1

    @staticmethod
    def _send(writer, data):
        frame = f"data: {data}\n\n".encode("utf-8")
        writer.write(b"%x\r\n" % len(frame) + frame + b"\r\n")

    async def _chat_once(self, writer, payload):
        self.stats.chat_nonstream += 1
        llm = self.llm
        await asyncio.sleep(llm["nonstream_first_ms"] / 1000 + llm["nonstream_tokens"] / llm["tokens_per_s"])
        messages = payload.get("messages", [])
        text = "".join(self._tokens(llm["nonstream_tokens"], ""))
        self._json(writer, 200, {
            "id": f"chatcmpl-{random.getrandbits(48):x}", "object": "chat.completion", "created": int(time.time()),
            "model": payload.get("model", "mock"),
            "choices": [{"index": 0, "message": {"role": "assistant", "content": text}, "finish_reason": "stop"}],
            "usage": {"prompt_tokens": 100 + len(messages), "completion_tokens": llm["nonstream_tokens"],
                      "total_tokens": 100 + len(messages) + llm["nonstream_tokens"]},
        })


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    ap = argparse.ArgumentParser()
    ap.add_argument("--scenario", default=os.path.join(here, "s1", "scenario.json"))
    ap.add_argument("--port", type=int)
    args = ap.parse_args()
    with open(args.scenario, encoding="utf-8") as f:
        sc = json.load(f)
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    srv = MockServer(sc, host=None, port=args.port or sc["ports"]["mock"])  # 전 인터페이스 - localhost 가 ::1 로 풀려도 받는다
    log.info("모의 OpenAI 기동 port=%s", srv.port)
    srv.serve_forever()


if __name__ == "__main__":
    main()
