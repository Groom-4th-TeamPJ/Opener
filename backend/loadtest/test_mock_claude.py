"""모의 Claude·Voyage 서버 시험 (행 MK-1, r129) - Anthropic Messages(스트림·비스트림)·Voyage 임베딩·프롬프트 검사기·장애 구간·거절 응답"""
import http.client
import json
import math
import os
import sys
import threading
import time
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import mock_claude  # noqa: E402

with open(os.path.join(HERE, "s1", "scenario.json"), encoding="utf-8") as f:
    SCENARIO = json.load(f)
LLM = SCENARIO["llm"]
DIM = 1024


def q(session, turn, text="질문"):
    return {"role": "user", "content": f"[q s={session} t={turn}] {text}"}


def a(session, turn, text="답변"):
    return {"role": "assistant", "content": f"[a s={session} t={turn}] {text}"}


SYSTEM_TEXT = "규칙 프롬프트 + 참고 자료"


def cosine(x, y):
    return sum(i * j for i, j in zip(x, y)) / (math.sqrt(sum(i * i for i in x)) * math.sqrt(sum(j * j for j in y)))


class PromptCheckerTest(unittest.TestCase):
    """Anthropic 형식: system 은 최상위 문자열이라 messages 에는 user·assistant 만 있다 (구형 system 역할 메시지도 건너뛴다)"""

    def test_r129_완전한_첫_턴_프롬프트는_통과(self):
        self.assertIsNone(mock_claude.check_prompt([q(7, 1)]))

    def test_r129_완전한_세_번째_턴_프롬프트는_통과(self):
        self.assertIsNone(mock_claude.check_prompt([q(7, 1), a(7, 1), q(7, 2), a(7, 2), q(7, 3)]))

    def test_r129_system_역할_메시지가_섞여_있어도_건너뛴다(self):
        self.assertIsNone(mock_claude.check_prompt([{"role": "system", "content": SYSTEM_TEXT}, q(7, 1)]))

    def test_이전_답변이_빠진_프롬프트를_잡는다(self):
        self.assertEqual("answer_missing", mock_claude.check_prompt([q(7, 1), q(7, 2)]))

    def test_현재_질문이_두_번_실린_프롬프트를_잡는다(self):
        self.assertEqual("current_question_twice", mock_claude.check_prompt([q(7, 1), a(7, 1), q(7, 2), q(7, 2)]))

    def test_이전_질문이_빠진_프롬프트를_잡는다(self):
        self.assertEqual("question_missing", mock_claude.check_prompt([a(7, 1), q(7, 2)]))

    def test_다른_세션의_메시지가_섞이면_잡는다(self):
        self.assertEqual("foreign_session", mock_claude.check_prompt([q(7, 1), a(8, 1), q(7, 2)]))

    def test_마지막_사용자_메시지에_표식이_없으면_잡는다(self):
        self.assertEqual("no_marker", mock_claude.check_prompt([q(7, 1), a(7, 1), {"role": "user", "content": "표식 없음"}]))

    def test_순서가_뒤바뀐_프롬프트를_잡는다(self):
        self.assertEqual("order_violation", mock_claude.check_prompt([a(7, 1), q(7, 1), q(7, 2)]))

    def test_질문_뒤에_다른_메시지가_오면_잡는다(self):
        self.assertEqual("current_question_not_last", mock_claude.check_prompt([q(7, 1), a(7, 1), q(7, 2), a(7, 2)]))

    def test_r129_본문이_text_블록_배열이어도_같은_판정(self):
        msgs = [{"role": "user", "content": [{"type": "text", "text": "[q s=7 t=1] 질문"}]}]
        self.assertIsNone(mock_claude.check_prompt(msgs))


class EmbeddingFunctionTest(unittest.TestCase):
    def test_r129_같은_글은_같은_벡터_1024개(self):
        v1, v2 = mock_claude.embedding_for("미적분 극한"), mock_claude.embedding_for("미적분 극한")
        self.assertEqual(DIM, len(v1))
        self.assertEqual(v1, v2)

    def test_다른_글은_다른_벡터(self):
        self.assertNotEqual(mock_claude.embedding_for("미적분 극한"), mock_claude.embedding_for("영어 문법"))

    def test_r129_벡터는_길이_1_이다(self):
        self.assertAlmostEqual(1.0, math.sqrt(sum(x * x for x in mock_claude.embedding_for("길이 확인"))), places=6)


def make_pool(n=8):
    """서로 거의 직교인 단위 벡터 n 개 - 실제 청크 벡터를 흉내 낸 검색 대상 풀"""
    pool = []
    for i in range(n):
        v = [0.0] * DIM
        v[i * 7] = 1.0
        pool.append(v)
    return pool


class QueryVectorTest(unittest.TestCase):
    """질문(input_type query) 임베딩 - 풀이 있으면 저장된 실제 청크 벡터 하나를 골라 그 근처 벡터를 돌려준다 (RAG 검색이 결과를 찾게 한다)"""

    def test_r129_풀의_벡터_하나와_코사인_0_9_이상이다(self):
        pool = make_pool()
        for text in ("대수 질문", "기하 질문", "확률 질문"):
            v = mock_claude.query_vector(text, pool)
            self.assertEqual(DIM, len(v))
            self.assertGreaterEqual(max(cosine(v, p) for p in pool), 0.9, text)

    def test_r129_같은_질문은_같은_벡터_다른_질문은_풀_안에서_골고루_고른다(self):
        pool = make_pool()
        self.assertEqual(mock_claude.query_vector("같은 질문", pool), mock_claude.query_vector("같은 질문", pool))
        picked = {max(range(len(pool)), key=lambda i: cosine(mock_claude.query_vector(f"질문 {k}", pool), pool[i])) for k in range(60)}
        self.assertGreaterEqual(len(picked), 6)

    def test_r129_풀이_없으면_글_해시_벡터로_대신한다(self):
        self.assertEqual(mock_claude.embedding_for("질문"), mock_claude.query_vector("질문", None))
        self.assertEqual(mock_claude.embedding_for("질문"), mock_claude.query_vector("질문", []))


def read_sse(resp):
    """SSE 응답을 (event, data dict) 목록으로 - 끝까지 읽는다"""
    events, name = [], None
    while True:
        line = resp.readline()
        if not line:
            break
        line = line.decode("utf-8").rstrip("\r\n")
        if line.startswith("event:"):
            name = line[6:].strip()
        elif line.startswith("data:"):
            events.append((name, json.loads(line[5:].strip())))
    return events


class ServerTest(unittest.TestCase):
    POOL = make_pool()

    @classmethod
    def setUpClass(cls):
        cls.server = mock_claude.MockServer(SCENARIO, port=0, query_pool=cls.POOL)
        cls.server.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.stop()

    def conn(self):
        return http.client.HTTPConnection("127.0.0.1", self.server.port, timeout=30)

    def post(self, path, payload):
        c = self.conn()
        c.request("POST", path, json.dumps(payload), {"Content-Type": "application/json"})
        return c, c.getresponse()

    def get_stats(self):
        c = self.conn()
        c.request("GET", "/stats")
        data = json.loads(c.getresponse().read())
        c.close()
        return data

    def reset(self):
        c = self.conn()
        c.request("POST", "/stats/reset")
        c.getresponse().read()
        c.close()

    def stream_body(self, session, turn, messages=None):
        return {"model": "claude-sonnet-5-5", "max_tokens": 8000, "stream": True, "system": SYSTEM_TEXT,
                "output_config": {"effort": "low"}, "messages": messages or [q(session, turn)]}

    def read_stream(self, session, turn, out, key):
        t0 = time.monotonic()
        c, resp = self.post("/v1/messages", self.stream_body(session, turn))
        events, first, last = [], None, None
        name = None
        while True:
            line = resp.readline()
            if not line:
                break
            line = line.decode("utf-8").rstrip("\r\n")
            if line.startswith("event:"):
                name = line[6:].strip()
            elif line.startswith("data:"):
                data = json.loads(line[5:].strip())
                events.append((name, data))
                if name == "content_block_delta":
                    now = time.monotonic()
                    first = first or now
                    last = now
        c.close()
        out[key] = ((first - t0) * 1000, last - first, events)

    def test_r129_스트림은_Anthropic_이벤트_순서로_오고_첫_토큰과_속도가_시나리오_값과_같다(self):
        self.reset()
        results = {}
        threads = [threading.Thread(target=self.read_stream, args=(1000 + i, 1, results, i)) for i in range(20)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()
        lo, hi = LLM["first_token_ms"]
        self.assertEqual(20, len(results))
        for i, (first_ms, span, events) in results.items():
            names = [n for n, _ in events]
            self.assertEqual(["message_start", "content_block_start"], names[:2], f"vu {i}")
            self.assertEqual(["content_block_stop", "message_delta", "message_stop"], names[-3:], f"vu {i}")
            deltas = [d for n, d in events if n == "content_block_delta"]
            self.assertEqual(LLM["answer_tokens"], len(deltas), f"vu {i}")
            self.assertEqual({"type": "text_delta"}, {"type": deltas[0]["delta"]["type"]})
            self.assertTrue(deltas[0]["delta"]["text"].startswith(f"[a s={1000 + i} t=1] "), deltas[0]["delta"]["text"][:30])
            self.assertEqual("end_turn", dict(events)["message_delta"]["delta"]["stop_reason"])
            self.assertEqual("message", dict(events)["message_start"]["message"]["type"])
            self.assertEqual([], dict(events)["message_start"]["message"]["content"])
            self.assertEqual(0, dict(events)["content_block_start"]["index"])
            self.assertEqual({"type": "text", "text": ""}, dict(events)["content_block_start"]["content_block"])
            self.assertGreaterEqual(first_ms, lo - 10, f"vu {i}")
            self.assertLessEqual(first_ms, hi + 100, f"vu {i}")
            rate = (len(deltas) - 1) / span
            self.assertAlmostEqual(LLM["tokens_per_s"], rate, delta=LLM["tokens_per_s"] * 0.10, msg=f"vu {i}")
        st = self.get_stats()
        self.assertEqual(20, st["chat_streams"])
        self.assertEqual(20, st["prompt_checked"])
        self.assertEqual(0, st["prompt_missing"])
        self.assertEqual(20, st["routes"]["messages_stream"])

    def test_r129_비스트림은_Message_JSON_을_한_번에_돌려준다(self):
        self.reset()
        t0 = time.monotonic()
        body = {"model": "claude-sonnet-5-5", "max_tokens": 8000, "system": SYSTEM_TEXT, "messages": [q(5, 3)]}
        c, resp = self.post("/v1/messages", body)
        msg = json.loads(resp.read())
        elapsed_ms = (time.monotonic() - t0) * 1000
        c.close()
        expected = LLM["nonstream_first_ms"] + LLM["nonstream_tokens"] / LLM["tokens_per_s"] * 1000
        self.assertAlmostEqual(expected, elapsed_ms, delta=300)
        self.assertEqual("message", msg["type"])
        self.assertEqual("assistant", msg["role"])
        self.assertEqual("end_turn", msg["stop_reason"])
        self.assertEqual("text", msg["content"][0]["type"])
        self.assertEqual(LLM["nonstream_tokens"] * LLM["chars_per_token"], len(msg["content"][0]["text"]))
        self.assertEqual(LLM["nonstream_tokens"], msg["usage"]["output_tokens"])
        st = self.get_stats()
        self.assertEqual(1, st["chat_nonstream"])
        self.assertEqual(0, st["chat_streams"])
        self.assertEqual(1, st["routes"]["messages"])

    def test_r129_비스트림_동시_호출_최대치를_센다(self):
        self.reset()
        done = []

        def once(i):
            c, resp = self.post("/v1/messages", {"model": "m", "max_tokens": 10, "messages": [q(i, 1)]})
            resp.read()
            c.close()
            done.append(i)

        threads = [threading.Thread(target=once, args=(i,)) for i in range(6)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()
        self.assertEqual(6, len(done))
        self.assertEqual(6, self.get_stats()["nonstream_inflight_peak"])
        self.reset()
        self.assertEqual(0, self.get_stats()["nonstream_inflight_peak"])

    def test_r129_Voyage_임베딩은_1024개_실수와_index_순서와_토큰_수를_150ms_뒤에_돌려준다(self):
        self.reset()
        t0 = time.monotonic()
        c, resp = self.post("/v1/embeddings", {"input": ["가나다", "라마바"], "model": "voyage-4", "output_dimension": 1024, "input_type": "document"})
        body = json.loads(resp.read())
        elapsed_ms = (time.monotonic() - t0) * 1000
        c.close()
        self.assertAlmostEqual(LLM["embedding_ms"], elapsed_ms, delta=30)
        self.assertEqual("list", body["object"])
        self.assertEqual([0, 1], [d["index"] for d in body["data"]])
        self.assertEqual(DIM, len(body["data"][0]["embedding"]))
        self.assertNotEqual(body["data"][0]["embedding"], body["data"][1]["embedding"])
        self.assertEqual("voyage-4", body["model"])
        self.assertGreater(body["usage"]["total_tokens"], 0)
        st = self.get_stats()
        self.assertEqual(1, st["embeddings"])
        self.assertEqual(1, st["routes"]["embeddings"])

    def test_r129_input_type_query_는_풀_벡터_근처를_document_는_글_해시를_돌려준다(self):
        self.reset()
        c, resp = self.post("/v1/embeddings", {"input": ["대수 질문"], "model": "voyage-4", "output_dimension": 1024, "input_type": "query"})
        qv = json.loads(resp.read())["data"][0]["embedding"]
        c.close()
        self.assertGreaterEqual(max(cosine(qv, p) for p in self.POOL), 0.9)
        c, resp = self.post("/v1/embeddings", {"input": ["대수 질문"], "model": "voyage-4", "output_dimension": 1024, "input_type": "document"})
        dv = json.loads(resp.read())["data"][0]["embedding"]
        c.close()
        self.assertEqual(mock_claude.embedding_for("대수 질문"), dv)
        st = self.get_stats()
        self.assertEqual({"query": 1, "document": 1}, st["embeddings_by_input_type"])

    def test_r129_이전_OpenAI_경로는_404(self):
        c, resp = self.post("/v1/chat/completions", {"model": "m", "stream": False, "messages": [q(1, 1)]})
        self.assertEqual(404, resp.status)
        resp.read()
        c.close()

    def test_r129_이전_답변이_빠진_스트림_요청이_집계에_사유와_함께_남는다(self):
        self.reset()
        c, resp = self.post("/v1/messages", self.stream_body(9, 2, [q(9, 1), q(9, 2)]))
        while True:  # 첫 본문 청크까지만 읽고 끊는다 (연결 종료에도 서버가 멀쩡해야 한다)
            line = resp.readline()
            if not line or b"[a s=9 t=2]" in line:
                break
        c.close()
        st = self.get_stats()
        self.assertEqual(1, st["prompt_checked"])
        self.assertEqual(1, st["prompt_missing"])
        self.assertEqual({"answer_missing": 1}, st["prompt_missing_by_reason"])

    def test_r129_본문이_청크_전송이어도_임베딩_입력을_읽는다(self):
        self.reset()
        raw = json.dumps({"input": ["가나다", "라마바", "사아자"], "input_type": "document"}).encode()
        c = self.conn()
        c.putrequest("POST", "/v1/embeddings")
        c.putheader("Transfer-Encoding", "chunked")
        c.putheader("Content-Type", "application/json")
        c.endheaders()
        c.send(b"%x\r\n" % 10 + raw[:10] + b"\r\n" + b"%x\r\n" % (len(raw) - 10) + raw[10:] + b"\r\n0\r\n\r\n")
        body = json.loads(c.getresponse().read())
        c.close()
        self.assertEqual(3, len(body["data"]))

    def test_r129_거절_예약은_다음_스트림_한_건만_stop_reason_refusal_로_끝낸다(self):
        self.reset()
        c, resp = self.post("/refuse", {"next": 1, "text_chunks": 2})
        self.assertEqual(200, resp.status)
        resp.read()
        c.close()
        c, resp = self.post("/v1/messages", self.stream_body(11, 1))
        events = read_sse(resp)
        c.close()
        deltas = [d for n, d in events if n == "content_block_delta"]
        self.assertEqual(2, len(deltas))
        final = dict(events)["message_delta"]
        self.assertEqual("refusal", final["delta"]["stop_reason"])
        self.assertEqual("message_stop", events[-1][0])
        c, resp = self.post("/v1/messages", self.stream_body(11, 2, [q(11, 1), a(11, 1), q(11, 2)]))
        events2 = read_sse(resp)
        c.close()
        self.assertEqual("end_turn", dict(events2)["message_delta"]["delta"]["stop_reason"])
        self.assertEqual(1, self.get_stats()["refusals_served"])

    def test_r129_거절_예약에_text_chunks_0_이면_본문_조각_없이_끝낸다(self):
        self.reset()
        c, resp = self.post("/refuse", {"next": 1, "text_chunks": 0})
        resp.read()
        c.close()
        c, resp = self.post("/v1/messages", self.stream_body(12, 1))
        events = read_sse(resp)
        c.close()
        self.assertEqual([], [d for n, d in events if n == "content_block_delta"])
        self.assertEqual("refusal", dict(events)["message_delta"]["delta"]["stop_reason"])

    def test_r129_stats_reset이_집계를_0으로_되돌린다(self):
        c, resp = self.post("/v1/embeddings", {"input": "x", "input_type": "document"})
        resp.read()
        c.close()
        self.reset()
        st = self.get_stats()
        for key in ("chat_streams", "chat_nonstream", "embeddings", "prompt_checked", "prompt_missing", "refusals_served"):
            self.assertEqual(0, st[key], key)
        self.assertEqual({"messages_stream": 0, "messages": 0, "embeddings": 0}, st["routes"])
        self.assertEqual({}, st["prompt_missing_by_reason"])


class FaultWindowTest(unittest.TestCase):
    """장애 구간 - 구간 안에서는 Messages(스트림·비스트림)와 Voyage 임베딩 세 경로가 HTTP 500 과 각 서비스 형식 오류 본문으로 답한다"""

    @classmethod
    def setUpClass(cls):
        cls.server = mock_claude.MockServer(SCENARIO, port=0)
        cls.server.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.stop()

    def setUp(self):
        self.call("POST", "/stats/reset")
        self.set_window(None, None)

    def call(self, method, path, payload=None):
        c = http.client.HTTPConnection("127.0.0.1", self.server.port, timeout=30)
        c.request(method, path, None if payload is None else json.dumps(payload), {"Content-Type": "application/json"})
        r = c.getresponse()
        body = json.loads(r.read() or b"null")
        c.close()
        return r.status, body

    def set_window(self, start_ms, end_ms):
        return self.call("POST", "/fault", {"start_ms": start_ms, "end_ms": end_ms})

    def window_around_now(self):
        now = time.time() * 1000
        self.set_window(now - 60_000, now + 60_000)

    def get_stats(self):
        return self.call("GET", "/stats")[1]

    CHAT = ("/v1/messages", {"model": "m", "max_tokens": 10, "messages": [q(5, 1)]})
    STREAM = ("/v1/messages", {"model": "m", "max_tokens": 10, "stream": True, "messages": [q(5, 1)]})
    EMBED = ("/v1/embeddings", {"input": "가나다", "input_type": "document"})

    def test_r129_fault_구간_안에서_세_경로가_500_과_각_서비스_형식_오류_본문으로_답한다(self):
        self.window_around_now()
        for name, (path, payload) in (("stream", self.STREAM), ("nonstream", self.CHAT)):
            status, body = self.call("POST", path, payload)
            self.assertEqual(500, status, name)
            self.assertEqual({"type": "error", "error": {"type": "api_error", "message": "mock fault"}}, body, name)
        status, body = self.call("POST", *self.EMBED)
        self.assertEqual(500, status)
        self.assertEqual({"detail": "mock fault"}, body)

    def test_r129_fault_구간_밖에서는_정상_응답(self):
        now = time.time() * 1000
        for start, end in ((now - 120_000, now - 60_000), (now + 60_000, now + 120_000)):
            self.set_window(start, end)
            status, body = self.call("POST", *self.EMBED)
            self.assertEqual(200, status)
            self.assertEqual(DIM, len(body["data"][0]["embedding"]))

    def test_r129_fault_구간을_안_정하면_정상_응답(self):
        self.assertEqual(200, self.call("POST", *self.EMBED)[0])

    def test_r129_fault_구간_끝_시각_이후와_빈_구간은_정상_응답(self):
        now = time.time() * 1000
        self.set_window(now - 50, now - 1)
        self.assertEqual(200, self.call("POST", *self.EMBED)[0])
        self.set_window(now + 60_000, now + 60_000)
        self.assertEqual(200, self.call("POST", *self.EMBED)[0])

    def test_r129_fault_stats_가_구간_시작_끝과_경로별_실패_수를_보고한다(self):
        self.window_around_now()
        self.call("POST", *self.EMBED)
        self.call("POST", *self.EMBED)
        self.call("POST", *self.CHAT)
        self.call("POST", *self.STREAM)
        f = self.get_stats()["fault"]
        self.assertEqual({"chat_stream": 1, "chat_nonstream": 1, "embeddings": 2}, f["failed_by_route"])
        self.assertIsInstance(f["start_ms"], (int, float))
        self.assertEqual(120_000, f["end_ms"] - f["start_ms"])

    def test_r129_fault_구간이_없으면_stats_의_시작_끝은_null_실패_수는_0(self):
        f = self.get_stats()["fault"]
        self.assertIsNone(f["start_ms"])
        self.assertIsNone(f["end_ms"])
        self.assertEqual({"chat_stream": 0, "chat_nonstream": 0, "embeddings": 0}, f["failed_by_route"])

    def test_r129_fault_로_실패시킨_요청은_정상_호출_집계에_넣지_않는다(self):
        self.window_around_now()
        for path, payload in (self.STREAM, self.CHAT, self.EMBED):
            self.call("POST", path, payload)
        st = self.get_stats()
        for key in ("chat_streams", "chat_nonstream", "embeddings", "prompt_checked"):
            self.assertEqual(0, st[key], key)

    def test_r129_fault_stats_reset_은_실패_수만_0으로_하고_구간은_둔다(self):
        self.window_around_now()
        self.call("POST", *self.EMBED)
        start = self.get_stats()["fault"]["start_ms"]
        self.call("POST", "/stats/reset")
        f = self.get_stats()["fault"]
        self.assertEqual(0, f["failed_by_route"]["embeddings"])
        self.assertEqual(start, f["start_ms"])

    def test_r129_fault_구간을_null_로_지우면_다시_정상_응답(self):
        self.window_around_now()
        self.assertEqual(500, self.call("POST", *self.EMBED)[0])
        self.set_window(None, None)
        self.assertEqual(200, self.call("POST", *self.EMBED)[0])


if __name__ == "__main__":
    unittest.main()
