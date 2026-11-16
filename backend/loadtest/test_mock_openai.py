"""모의 OpenAI 서버 시험 (행 MK-1) - 지연·토큰 속도·임베딩·프롬프트 검사기"""
import http.client
import json
import os
import sys
import threading
import time
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import mock_openai  # noqa: E402

with open(os.path.join(HERE, "s1", "scenario.json"), encoding="utf-8") as f:
    SCENARIO = json.load(f)
LLM = SCENARIO["llm"]


def q(session, turn, text="질문"):
    return {"role": "user", "content": f"[q s={session} t={turn}] {text}"}


def a(session, turn, text="답변"):
    return {"role": "assistant", "content": f"[a s={session} t={turn}] {text}"}


SYSTEM = {"role": "system", "content": "규칙 프롬프트 + 참고 자료"}


class PromptCheckerTest(unittest.TestCase):
    def test_완전한_첫_턴_프롬프트는_통과(self):
        self.assertIsNone(mock_openai.check_prompt([SYSTEM, q(7, 1)]))

    def test_완전한_세_번째_턴_프롬프트는_통과(self):
        msgs = [SYSTEM, q(7, 1), a(7, 1), q(7, 2), a(7, 2), q(7, 3)]
        self.assertIsNone(mock_openai.check_prompt(msgs))

    def test_이전_답변이_빠진_프롬프트를_잡는다(self):
        self.assertEqual("answer_missing", mock_openai.check_prompt([SYSTEM, q(7, 1), q(7, 2)]))

    def test_현재_질문이_두_번_실린_프롬프트를_잡는다(self):
        msgs = [SYSTEM, q(7, 1), a(7, 1), q(7, 2), q(7, 2)]
        self.assertEqual("current_question_twice", mock_openai.check_prompt(msgs))

    def test_이전_질문이_빠진_프롬프트를_잡는다(self):
        self.assertEqual("question_missing", mock_openai.check_prompt([SYSTEM, a(7, 1), q(7, 2)]))

    def test_다른_세션의_메시지가_섞이면_잡는다(self):
        msgs = [SYSTEM, q(7, 1), a(8, 1), q(7, 2)]
        self.assertEqual("foreign_session", mock_openai.check_prompt(msgs))

    def test_마지막_사용자_메시지에_표식이_없으면_잡는다(self):
        msgs = [SYSTEM, q(7, 1), a(7, 1), {"role": "user", "content": "표식 없음"}]
        self.assertEqual("no_marker", mock_openai.check_prompt(msgs))

    def test_순서가_뒤바뀐_프롬프트를_잡는다(self):
        msgs = [SYSTEM, a(7, 1), q(7, 1), q(7, 2)]
        self.assertEqual("order_violation", mock_openai.check_prompt(msgs))

    def test_질문_뒤에_다른_메시지가_오면_잡는다(self):
        msgs = [SYSTEM, q(7, 1), a(7, 1), q(7, 2), a(7, 2)]
        self.assertEqual("current_question_not_last", mock_openai.check_prompt(msgs))

    def test_본문이_배열_형태여도_같은_판정(self):
        msgs = [SYSTEM, {"role": "user", "content": [{"type": "text", "text": "[q s=7 t=1] 질문"}]}]
        self.assertIsNone(mock_openai.check_prompt(msgs))


class EmbeddingFunctionTest(unittest.TestCase):
    def test_같은_글은_같은_벡터_1536개(self):
        v1 = mock_openai.embedding_for("미적분 극한")
        v2 = mock_openai.embedding_for("미적분 극한")
        self.assertEqual(1536, len(v1))
        self.assertEqual(v1, v2)

    def test_다른_글은_다른_벡터(self):
        self.assertNotEqual(mock_openai.embedding_for("미적분 극한"), mock_openai.embedding_for("영어 문법"))

    def test_모든_벡터의_코사인_유사도가_임계값_0_2를_넘는다(self):
        # RAG 검색 유사도 임계값 0.2 아래로 떨어지면 검색이 0건이 되어 운영 경로와 달라진다
        a_, b_ = mock_openai.embedding_for("완전히 무관한 첫 글"), mock_openai.embedding_for("다른 둘째 문장입니다")
        dot = sum(x * y for x, y in zip(a_, b_))
        norm = (sum(x * x for x in a_) ** 0.5) * (sum(y * y for y in b_) ** 0.5)
        self.assertGreater(dot / norm, 0.2)


class ServerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = mock_openai.MockServer(SCENARIO, port=0)
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

    def stats(self):
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

    def read_stream(self, session, turn, out, key):
        # (첫 토큰까지 ms, 본문 청크 수, 첫~끝 토큰 간격 초, 이어붙인 본문, [DONE] 여부)
        payload = {"model": "gpt-4.1", "stream": True, "messages": [SYSTEM, q(session, turn)]}
        t0 = time.monotonic()
        c, resp = self.post("/v1/chat/completions", payload)
        first = last = None
        chunks, text, done = 0, [], False
        while True:
            line = resp.readline()
            if not line:
                break
            line = line.decode("utf-8").strip()
            if not line.startswith("data:"):
                continue
            body = line[5:].strip()
            if body == "[DONE]":
                done = True
                break
            content = json.loads(body)["choices"][0]["delta"].get("content")
            if content:
                now = time.monotonic()
                first = first or now
                last = now
                chunks += 1
                text.append(content)
        c.close()
        out[key] = ((first - t0) * 1000, chunks, last - first, "".join(text), done)

    def test_스트림_첫_토큰과_토큰_속도가_시나리오_값과_같다(self):
        self.reset()
        results = {}
        threads = [threading.Thread(target=self.read_stream, args=(1000 + i, 1, results, i)) for i in range(20)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()
        lo, hi = LLM["first_token_ms"]
        self.assertEqual(20, len(results))
        for i, (first_ms, chunks, span, text, done) in results.items():
            self.assertGreaterEqual(first_ms, lo - 10, f"vu {i}")
            self.assertLessEqual(first_ms, hi + 100, f"vu {i}")
            self.assertEqual(LLM["answer_tokens"], chunks, f"vu {i}")
            rate = (chunks - 1) / span
            self.assertAlmostEqual(LLM["tokens_per_s"], rate, delta=LLM["tokens_per_s"] * 0.10, msg=f"vu {i}")
            self.assertTrue(text.startswith(f"[a s={1000 + i} t=1] "), text[:30])
            self.assertTrue(done)
        st = self.stats()
        self.assertEqual(20, st["chat_streams"])
        self.assertEqual(20, st["prompt_checked"])
        self.assertEqual(0, st["prompt_missing"])

    def test_비스트림_완성은_1_5초_뒤_150토큰_분량을_한_번에_돌려준다(self):
        self.reset()
        t0 = time.monotonic()
        c, resp = self.post("/v1/chat/completions", {"model": "m", "stream": False, "messages": [SYSTEM, q(5, 3)]})
        body = json.loads(resp.read())
        elapsed_ms = (time.monotonic() - t0) * 1000
        c.close()
        expected = LLM["nonstream_first_ms"] + LLM["nonstream_tokens"] / LLM["tokens_per_s"] * 1000
        self.assertAlmostEqual(expected, elapsed_ms, delta=300)
        content = body["choices"][0]["message"]["content"]
        self.assertEqual(LLM["nonstream_tokens"] * LLM["chars_per_token"], len(content))
        st = self.stats()
        self.assertEqual(1, st["chat_nonstream"])
        self.assertEqual(0, st["chat_streams"])

    def test_비스트림_동시_호출_최대치를_센다(self):
        self.reset()
        results = []

        def once(i):
            c, resp = self.post("/v1/chat/completions", {"model": "m", "stream": False, "messages": [SYSTEM, q(i, 1)]})
            resp.read()
            c.close()
            results.append(i)

        threads = [threading.Thread(target=once, args=(i,)) for i in range(6)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()
        st = self.stats()
        self.assertEqual(6, len(results))
        self.assertEqual(6, st["nonstream_inflight_peak"])
        self.reset()
        self.assertEqual(0, self.stats()["nonstream_inflight_peak"])

    def test_임베딩은_1536개_실수를_150ms_뒤에_돌려준다(self):
        self.reset()
        t0 = time.monotonic()
        c, resp = self.post("/v1/embeddings", {"model": "text-embedding-3-small", "input": ["가나다", "라마바"]})
        body = json.loads(resp.read())
        elapsed_ms = (time.monotonic() - t0) * 1000
        c.close()
        self.assertAlmostEqual(LLM["embedding_ms"], elapsed_ms, delta=30)
        self.assertEqual(2, len(body["data"]))
        self.assertEqual(1536, len(body["data"][0]["embedding"]))
        self.assertNotEqual(body["data"][0]["embedding"], body["data"][1]["embedding"])
        self.assertEqual(1, self.stats()["embeddings"])

    def test_이전_답변이_빠진_스트림_요청이_집계에_사유와_함께_남는다(self):
        self.reset()
        payload = {"model": "m", "stream": True, "messages": [SYSTEM, q(9, 1), q(9, 2)]}
        c, resp = self.post("/v1/chat/completions", payload)
        while True:  # 첫 본문 청크까지만 읽고 끊는다 (연결 종료에도 서버가 멀쩡해야 한다)
            line = resp.readline()
            if not line or b'"content":"[a s=9 t=2]' in line.replace(b" ", b"") or b"[a s=9 t=2]" in line:
                break
        c.close()
        st = self.stats()
        self.assertEqual(1, st["prompt_checked"])
        self.assertEqual(1, st["prompt_missing"])
        self.assertEqual({"answer_missing": 1}, st["prompt_missing_by_reason"])

    def test_본문이_청크_전송이어도_임베딩_입력을_읽는다(self):
        self.reset()
        raw = json.dumps({"input": ["가나다", "라마바", "사아자"]}).encode()
        c = self.conn()
        c.putrequest("POST", "/v1/embeddings")
        c.putheader("Transfer-Encoding", "chunked")
        c.putheader("Content-Type", "application/json")
        c.endheaders()
        c.send(b"%x\r\n" % 10 + raw[:10] + b"\r\n" + b"%x\r\n" % (len(raw) - 10) + raw[10:] + b"\r\n0\r\n\r\n")
        body = json.loads(c.getresponse().read())
        c.close()
        self.assertEqual(3, len(body["data"]))

    def test_stats_reset이_집계를_0으로_되돌린다(self):
        c, resp = self.post("/v1/embeddings", {"input": "x"})
        resp.read()
        c.close()
        self.reset()
        st = self.stats()
        for key in ("chat_streams", "chat_nonstream", "embeddings", "prompt_checked", "prompt_missing"):
            self.assertEqual(0, st[key], key)
        self.assertEqual({}, st["prompt_missing_by_reason"])


FAULT_BODY = {"error": {"message": "mock fault", "type": "server_error", "param": None, "code": None}}


class FaultWindowTest(unittest.TestCase):
    """장애 구간 - 구간 안에서는 채팅(스트림·비스트림)과 임베딩이 HTTP 500 과 OpenAI 형식 오류 본문으로 답한다"""

    @classmethod
    def setUpClass(cls):
        cls.server = mock_openai.MockServer(SCENARIO, port=0)
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

    def stats(self):
        return self.call("GET", "/stats")[1]

    CHAT = ("/v1/chat/completions", {"model": "m", "stream": False, "messages": [SYSTEM, q(5, 1)]})
    STREAM = ("/v1/chat/completions", {"model": "m", "stream": True, "messages": [SYSTEM, q(5, 1)]})
    EMBED = ("/v1/embeddings", {"input": "가나다"})

    def test_fault_구간_안에서_세_경로가_500_과_오류_본문으로_답한다(self):
        self.window_around_now()
        for name, (path, payload) in (("stream", self.STREAM), ("nonstream", self.CHAT), ("embeddings", self.EMBED)):
            status, body = self.call("POST", path, payload)
            self.assertEqual(500, status, name)
            self.assertEqual(FAULT_BODY, body, name)

    def test_fault_구간_밖에서는_정상_응답(self):
        now = time.time() * 1000
        for start, end in ((now - 120_000, now - 60_000), (now + 60_000, now + 120_000)):
            self.set_window(start, end)
            status, body = self.call("POST", *self.EMBED)
            self.assertEqual(200, status)
            self.assertEqual(1536, len(body["data"][0]["embedding"]))

    def test_fault_구간을_안_정하면_정상_응답(self):
        self.assertEqual(200, self.call("POST", *self.EMBED)[0])

    def test_fault_구간_끝_시각_이후와_빈_구간은_정상_응답(self):
        now = time.time() * 1000
        self.set_window(now - 50, now - 1)
        self.assertEqual(200, self.call("POST", *self.EMBED)[0])
        self.set_window(now + 60_000, now + 60_000)
        self.assertEqual(200, self.call("POST", *self.EMBED)[0])

    def test_fault_stats_가_구간_시작_끝과_경로별_실패_수를_보고한다(self):
        self.window_around_now()
        self.call("POST", *self.EMBED)
        self.call("POST", *self.EMBED)
        self.call("POST", *self.CHAT)
        self.call("POST", *self.STREAM)
        f = self.stats()["fault"]
        self.assertEqual({"chat_stream": 1, "chat_nonstream": 1, "embeddings": 2}, f["failed_by_route"])
        self.assertIsInstance(f["start_ms"], (int, float))
        self.assertEqual(120_000, f["end_ms"] - f["start_ms"])

    def test_fault_구간이_없으면_stats_의_시작_끝은_null_실패_수는_0(self):
        f = self.stats()["fault"]
        self.assertIsNone(f["start_ms"])
        self.assertIsNone(f["end_ms"])
        self.assertEqual({"chat_stream": 0, "chat_nonstream": 0, "embeddings": 0}, f["failed_by_route"])

    def test_fault_로_실패시킨_요청은_정상_호출_집계에_넣지_않는다(self):
        self.window_around_now()
        for path, payload in (self.STREAM, self.CHAT, self.EMBED):
            self.call("POST", path, payload)
        st = self.stats()
        self.assertEqual(0, st["chat_streams"])
        self.assertEqual(0, st["chat_nonstream"])
        self.assertEqual(0, st["embeddings"])
        self.assertEqual(0, st["prompt_checked"])

    def test_fault_stats_reset_은_실패_수만_0으로_하고_구간은_둔다(self):
        self.window_around_now()
        self.call("POST", *self.EMBED)
        start = self.stats()["fault"]["start_ms"]
        self.call("POST", "/stats/reset")
        f = self.stats()["fault"]
        self.assertEqual(0, f["failed_by_route"]["embeddings"])
        self.assertEqual(start, f["start_ms"])

    def test_fault_구간을_null_로_지우면_다시_정상_응답(self):
        self.window_around_now()
        self.assertEqual(500, self.call("POST", *self.EMBED)[0])
        self.set_window(None, None)
        self.assertEqual(200, self.call("POST", *self.EMBED)[0])


if __name__ == "__main__":
    unittest.main()
