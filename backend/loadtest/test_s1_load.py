"""S1 부하 생성기 시험 (행 LG-1) - SSE 프레임 조립·백분위·생각 시간·오류 집계·측정 구간"""
import asyncio
import json
import os
import queue
import random
import sys
import threading
import time
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import s1_load  # noqa: E402


def sse_bytes(event, data):
    return f"event:{event}\ndata:{data}\n\n".encode("utf-8")


class SseParserTest(unittest.TestCase):
    STREAM = (b": ping\n\n" + sse_bytes("connected", '{"type":"connected"}')
              + sse_bytes("message", '{"type":"chunk","chunk":"[a s=1 t=1] 가나"}')
              + b"event:complete\ndata:{\"a\":\ndata:1}\n\n")

    def parse_all(self, pieces):
        p = s1_load.SseParser()
        out = []
        for piece in pieces:
            out.extend(p.feed(piece))
        return out

    def test_한_번에_들어온_스트림을_이벤트_3개로_나눈다(self):
        events = self.parse_all([self.STREAM])
        self.assertEqual(["connected", "message", "complete"], [e for e, _ in events])
        self.assertEqual('{"a":\n1}', events[2][1])  # 여러 줄 data 는 줄바꿈으로 잇는다

    def test_어느_바이트에서_끊겨도_같은_이벤트가_나온다(self):
        whole = self.parse_all([self.STREAM])
        for cut in range(1, len(self.STREAM)):
            split = self.parse_all([self.STREAM[:cut], self.STREAM[cut:]])
            self.assertEqual(whole, split, f"cut={cut}")

    def test_한_바이트씩_들어와도_같은_이벤트가_나온다(self):
        whole = self.parse_all([self.STREAM])
        self.assertEqual(whole, self.parse_all([bytes([b]) for b in self.STREAM]))

    def test_한글_멀티바이트가_읽기_경계에서_끊겨도_깨지지_않는다(self):
        raw = sse_bytes("message", '{"chunk":"가나다"}')
        cut = raw.index("나".encode()) + 1
        events = self.parse_all([raw[:cut], raw[cut:]])
        self.assertEqual('{"chunk":"가나다"}', events[0][1])

    def test_CRLF_줄바꿈도_받는다(self):
        events = self.parse_all([b"event: complete\r\ndata: {}\r\n\r\n"])
        self.assertEqual([("complete", "{}")], events)


class MathTest(unittest.TestCase):
    def test_알려진_목록의_백분위(self):
        values = list(range(1, 101))
        random.Random(1).shuffle(values)
        self.assertEqual(50, s1_load.percentile(values, 50))
        self.assertEqual(95, s1_load.percentile(values, 95))
        self.assertEqual(99, s1_load.percentile(values, 99))
        self.assertEqual(7, s1_load.percentile([7], 99))
        self.assertIsNone(s1_load.percentile([], 50))

    def test_생각_시간은_10에서_20초_안에_고르게_나온다(self):
        rng = random.Random(3)
        draws = [s1_load.think_time(rng, [10, 20]) for _ in range(5000)]
        self.assertGreaterEqual(min(draws), 10)
        self.assertLessEqual(max(draws), 20)
        self.assertLess(min(draws), 10.1)
        self.assertGreater(max(draws), 19.9)
        self.assertAlmostEqual(15, sum(draws) / len(draws), delta=0.3)

    def test_턴은_시작과_끝이_모두_측정_구간_안일_때만_센다(self):
        w = (100.0, 200.0)
        self.assertTrue(s1_load.in_window(100.0, 200.0, w))
        self.assertFalse(s1_load.in_window(99.9, 150.0, w))
        self.assertFalse(s1_load.in_window(150.0, 200.1, w))

    def test_질문_본문은_표식으로_시작한다(self):
        q = s1_load.question_text(4321, 3, "[q s={session} t={turn}] ", random.Random(0))
        self.assertTrue(q.startswith("[q s=4321 t=3] "), q)
        self.assertGreater(len(q), 20)


class FakeApp:
    """채팅 서버 대역 - mode 에 따라 정상·SSE error 이벤트·HTTP 500·무응답·표식 누락을 흉내 낸다"""

    def __init__(self, mode):
        self.mode = mode
        self.queues = {}
        self.turns = {}
        self.counts = {"login": 0, "message": 0, "save": 0, "disconnect": 0}
        outer = self

        class H(BaseHTTPRequestHandler):
            protocol_version = "HTTP/1.1"

            def log_message(self, *a):
                pass

            def reply(self, status, body=b"", extra=()):
                self.send_response(status)
                for k, v in extra:
                    self.send_header(k, v)
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def do_POST(self):
                body = self.rfile.read(int(self.headers.get("Content-Length", 0)))
                if self.path.endswith("/auth/form-login"):
                    outer.counts["login"] += 1
                    self.reply(200, b"{}", [("Set-Cookie", "accessToken=tok; Path=/; HttpOnly")])
                elif self.path.endswith("/chat/message"):
                    outer.counts["message"] += 1
                    if outer.mode == "http500":
                        return self.reply(500, b"{}")
                    sid = json.loads(body)["sessionId"]
                    outer.turns[sid] = outer.turns.get(sid, 0) + 1
                    outer.queues[sid].put(outer.turns[sid])
                    self.reply(200)
                elif "/chat/save-message" in self.path:
                    outer.counts["save"] += 1
                    self.reply(200)
                elif "/chat/disconnect" in self.path:
                    outer.counts["disconnect"] += 1
                    sid = int(self.path.split("sessionId=")[1])
                    outer.queues[sid].put(None)
                    self.reply(200)
                else:
                    self.reply(404)

            def frag(self, data, size=0):
                step = size or len(data)
                for i in range(0, len(data), step):
                    piece = data[i:i + step]
                    self.wfile.write(b"%x\r\n" % len(piece) + piece + b"\r\n")
                    self.wfile.flush()
                    if size:
                        time.sleep(0.002)

            def do_GET(self):
                sid = int(self.path.split("sessionId=")[1])
                q = outer.queues.setdefault(sid, queue.Queue())
                self.send_response(200)
                self.send_header("Content-Type", "text/event-stream")
                self.send_header("Transfer-Encoding", "chunked")
                self.end_headers()
                self.frag(sse_bytes("connected", json.dumps({"type": "connected", "sessionId": str(sid)})))
                while True:
                    turn = q.get()
                    if turn is None:
                        break
                    if outer.mode == "silent":
                        continue
                    if outer.mode == "error_event":
                        self.frag(sse_bytes("error", json.dumps({"type": "error", "errorCode": "L_001"})))
                        continue
                    head = f"[a s={sid} t={turn}] " if outer.mode != "wrong_marker" else "[a s=0 t=0] "
                    for i in range(3):
                        chunk = (head if i == 0 else "") + "가나"
                        data = json.dumps({"type": "chunk", "sessionId": str(sid), "chunk": chunk}, ensure_ascii=False)
                        self.frag(sse_bytes("message", data), size=7)  # 한 이벤트를 7바이트 조각으로 쪼개 보낸다
                        time.sleep(0.01)
                    self.frag(sse_bytes("complete", json.dumps({"type": "complete", "sessionId": str(sid)})))
                self.wfile.write(b"0\r\n\r\n")
                self.close_connection = True

        self.httpd = ThreadingHTTPServer(("127.0.0.1", 0), H)
        self.httpd.daemon_threads = True
        self.port = self.httpd.server_address[1]
        threading.Thread(target=self.httpd.serve_forever, daemon=True).start()

    def stop(self):
        self.httpd.shutdown()
        self.httpd.server_close()


def vu_spec(port, **over):
    spec = {
        "vu": 0, "host": "127.0.0.1", "port": port, "prefix": "/api",
        "email": "u0@r125.test", "password": "pw",
        "sessions": [{"session_id": 501, "question_result_id": 9001}, {"session_id": 502, "question_result_id": 9002}],
        "turns_per_session": 3, "think_s": [0.0, 0.02], "turn_timeout_s": 5.0,
        "window": (0.0, 9e12), "stop_at": None, "max_sessions": 1, "seed": 1,
        "markers": {"question": "[q s={session} t={turn}] ", "answer": "[a s={session} t={turn}] "},
    }
    spec.update(over)
    return spec


def run(spec):
    rec = s1_load.Recorder()
    asyncio.run(s1_load.run_vu(spec, rec))
    return rec


class VuFlowTest(unittest.TestCase):
    def with_app(self, mode, **over):
        app = FakeApp(mode)
        try:
            return app, run(vu_spec(app.port, **over))
        finally:
            app.stop()

    def test_정상_흐름은_세_턴을_기록하고_저장_해제까지_호출한다(self):
        app, rec = self.with_app("ok")
        self.assertEqual(3, len(rec.turns))
        for t in rec.turns:
            self.assertTrue(t["ok"], t)
            self.assertIsNone(t["error"])
            self.assertGreater(t["ttft_ms"], 0)
            self.assertGreaterEqual(t["turn_ms"], t["ttft_ms"])
            self.assertTrue(t["in_window"])
        self.assertEqual(0, sum(rec.errors.values()))
        self.assertEqual({"login": 1, "message": 3, "save": 1, "disconnect": 1}, app.counts)
        self.assertEqual(1, len(rec.sessions))

    def test_SSE_error_이벤트는_오류_1건으로_센다(self):
        _, rec = self.with_app("error_event", turns_per_session=1)
        self.assertEqual(1, rec.errors["sse_error_event"])
        self.assertFalse(rec.turns[0]["ok"])
        self.assertEqual("sse_error_event", rec.turns[0]["error"])

    def test_HTTP_5xx_는_오류_1건으로_센다(self):
        _, rec = self.with_app("http500", turns_per_session=1)
        self.assertEqual(1, rec.errors["http_5xx"])
        self.assertFalse(rec.turns[0]["ok"])

    def test_종료_이벤트가_제한_시간_안에_안_오면_오류(self):
        _, rec = self.with_app("silent", turns_per_session=1, turn_timeout_s=0.4)
        self.assertEqual(1, rec.errors["turn_timeout"])

    def test_첫_토큰에_이_세션의_답변_표식이_없으면_오류(self):
        _, rec = self.with_app("wrong_marker", turns_per_session=1)
        self.assertEqual(1, rec.errors["marker_missing"])

    def test_측정_구간_밖에서_끝난_턴은_구간_밖으로_표시한다(self):
        now = time.time()
        _, rec = self.with_app("ok", turns_per_session=2, window=(now + 3600, now + 7200))
        self.assertEqual(2, len(rec.turns))
        self.assertFalse(any(t["in_window"] for t in rec.turns))


if __name__ == "__main__":
    unittest.main()
