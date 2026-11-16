"""수집기 순수 함수 시험 - commandstats 파싱·스크립트 내부 명령 제외·분위수·프로세스 CPU 시간·턴 통계"""
import os
import sys
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

import collect_s1  # noqa: E402

COMMANDSTATS = """# Commandstats
cmdstat_rpush:calls=20,usec=60,usec_per_call=3.00,rejected_calls=0,failed_calls=0
cmdstat_cluster|nodes:calls=7,usec=700,usec_per_call=100.00,rejected_calls=0,failed_calls=0
cmdstat_evalsha:calls=10,usec=900,usec_per_call=90.00,rejected_calls=0,failed_calls=0
"""


class ParseTest(unittest.TestCase):
    def test_commandstats_는_명령별_횟수와_마이크로초를_돌려준다(self):
        stats = collect_s1.parse_commandstats(COMMANDSTATS)
        self.assertEqual({"calls": 20, "usec": 60, "failed": 0}, stats["rpush"])
        self.assertEqual(7, stats["cluster|nodes"]["calls"])
        self.assertEqual(3, len(stats))

    def test_프로메테우스_본문을_이름과_라벨로_푼다(self):
        text = ('# HELP x\n# TYPE x counter\n'
                'chat_redis_read_seconds_count{application="backend",route="replica",} 12.0\n'
                'chat_redis_read_seconds_count{application="backend",route="master",} 3.0\n'
                'process_cpu_usage{application="backend"} 0.25\n')
        prom = collect_s1.parse_prom(text)
        self.assertEqual(12.0, collect_s1.prom_value(prom, "chat_redis_read_seconds_count", route="replica"))
        self.assertEqual(15.0, collect_s1.prom_sum(prom, "chat_redis_read_seconds_count"))
        self.assertEqual(0.25, collect_s1.prom_value(prom, "process_cpu_usage"))
        self.assertIsNone(collect_s1.prom_value(prom, "chat_redis_history_fallback_total"))

    def test_ps_CPU_시간_형식을_초로_바꾼다(self):
        self.assertAlmostEqual(7.77, collect_s1.parse_ps_time("0:07.77"))
        self.assertAlmostEqual(3723.5, collect_s1.parse_ps_time("1:02:03.50"))
        self.assertAlmostEqual(86400 + 3723.0, collect_s1.parse_ps_time("1-01:02:03.00"))


class QuantileTest(unittest.TestCase):
    def test_누적_버킷에서_95분위를_선형_보간한다(self):
        buckets = {0.1: 50, 0.2: 90, 0.5: 100, float("inf"): 100}
        self.assertAlmostEqual(0.1, collect_s1.quantile_from_buckets(buckets, 0.5), places=6)
        # 95% = 95번째 -> 0.2 와 0.5 사이 (90 -> 100 구간의 절반)
        self.assertAlmostEqual(0.35, collect_s1.quantile_from_buckets(buckets, 0.95), places=6)

    def test_표본이_없으면_None(self):
        self.assertIsNone(collect_s1.quantile_from_buckets({0.1: 0, float("inf"): 0}, 0.95))


class RoundTripRuleTest(unittest.TestCase):
    WRITES = {"rpush", "expire", "pexpireat", "hset", "hsetnx", "del"}

    def test_마스터는_스크립트_안에서_실행된_명령을_뺀다(self):
        # 세션 선점 스크립트 10번(내부: exists·hsetnx·hset·expire 각 1) + 앱이 직접 보낸 rpush 20·expire 40
        calls = {"evalsha": 10, "exists": 10, "hsetnx": 10, "hset": 10, "expire": 50, "rpush": 20, "info": 4}
        self.assertEqual(10 + 40 + 20, collect_s1.client_round_trips("master", calls, self.WRITES))

    def test_저장_스크립트가_더_있으면_내부_rpush와_expire_2개를_뺀다(self):
        # 선점 10번 + 저장 30번 (저장 내부: rpush 1 · expire 2)
        calls = {"evalsha": 40, "exists": 10, "hsetnx": 10, "hset": 10, "expire": 10 + 60, "rpush": 30}
        self.assertEqual(40, collect_s1.client_round_trips("master", calls, self.WRITES))

    def test_EVALSHA_가_NOSCRIPT_로_실패해_EVAL_로_다시_보낸_호출은_스크립트_한_번으로_센다(self):
        # 선점 2번: 호출마다 evalsha(실패) + eval(성공) -> 왕복 4, 내부 명령은 2번 분량만 뺀다
        calls = {"evalsha": 2, "eval": 2, "exists": 2, "hsetnx": 2, "hset": 2, "expire": 2}
        self.assertEqual(4, collect_s1.client_round_trips("master", calls, self.WRITES, {"evalsha": 2}))

    def test_복제본은_스크립트_효과를_감싼_multi_exec_도_제외한다(self):
        calls = {"multi": 3, "exec": 3, "hsetnx": 3, "hget": 13}
        self.assertEqual(13, collect_s1.client_round_trips("slave", calls, self.WRITES))

    def test_복제본은_쓰기_명령을_제외한_나머지만_센다(self):
        calls = {"rpush": 20, "pexpireat": 10, "lrange": 10, "readonly": 10, "ping": 2, "info": 6, "replconf": 5}
        self.assertEqual(10 + 10 + 2, collect_s1.client_round_trips("slave", calls, self.WRITES))

    def test_내부_명령_차감은_0_아래로_내려가지_않는다(self):
        calls = {"evalsha": 5, "hsetnx": 5}
        self.assertEqual(5, collect_s1.client_round_trips("master", calls, self.WRITES))


class VusPeakTest(unittest.TestCase):
    def test_vus_peak_는_관찰한_SSE_연결_최댓값(self):
        samples = [{"sse_active": 3.0}, {"sse_active": None}, {"sse_active": 7.0}, {"sse_active": 5.0}]
        self.assertEqual(7, collect_s1.vus_peak(samples))

    def test_표집이_없거나_값이_없으면_0(self):
        self.assertEqual(0, collect_s1.vus_peak([]))
        self.assertEqual(0, collect_s1.vus_peak([{"sse_active": None}]))


class HistoryFallbackTest(unittest.TestCase):
    def prom(self, **metrics):
        return {(n, ()): v for n, v in metrics.items()}

    def test_카운터가_있으면_구간_증가분(self):
        p0 = self.prom(chat_redis_history_fallback_total=2.0)
        p1 = self.prom(chat_redis_history_fallback_total=7.0)
        self.assertEqual(5, collect_s1.history_fallback(p0, p1, "new"))

    def test_새_코드에서_카운터가_없으면_한_번도_안_늘어난_0(self):
        # Micrometer 카운터는 처음 증가할 때 등록된다
        self.assertEqual(0, collect_s1.history_fallback({}, {}, "new"))

    def test_옛_코드에서_카운터가_없으면_null(self):
        self.assertIsNone(collect_s1.history_fallback({}, {}, "base"))


class TurnStatsTest(unittest.TestCase):
    def test_측정_구간의_성공_턴만_집계한다(self):
        turns = [
            {"ok": True, "in_window": True, "ttft_ms": 1000.0, "turn_ms": 9000.0},
            {"ok": True, "in_window": True, "ttft_ms": 2000.0, "turn_ms": 10000.0},
            {"ok": True, "in_window": False, "ttft_ms": 5.0, "turn_ms": 5.0},
            {"ok": False, "in_window": True, "ttft_ms": None, "turn_ms": 60000.0},
        ]
        st = collect_s1.turn_stats(turns)
        self.assertEqual(2, st["turns_completed"])
        self.assertEqual(3, st["attempts_in_window"])
        self.assertEqual(1000.0, st["ttft_ms"]["p50"])
        self.assertEqual(10000.0, st["turn_ms"]["p99"])

    def test_오류율은_측정_구간_오류_수를_구간_시도_수로_나눈다(self):
        window = (100.0, 200.0)
        errors = [{"t": 99.0, "kind": "login_failed"}, {"t": 150.0, "kind": "http_5xx"}, {"t": 160.0, "kind": "turn_timeout"}]
        self.assertAlmostEqual(0.5, collect_s1.errors_rate(errors, window, attempts=4))
        self.assertIsNone(collect_s1.errors_rate(errors, window, attempts=0))

class DrainTest(unittest.TestCase):
    def test_처음_빈_표본의_순번에_5초를_곱한_시간이_드레인_시간(self):
        series = [{"save": 9, "summary": 2}, {"save": 4, "summary": 1}, {"save": 0, "summary": 0}, {"save": 0, "summary": 0}]
        self.assertEqual(10, collect_s1.drain_seconds(series))

    def test_첫_표본이_이미_비었으면_0(self):
        self.assertEqual(0, collect_s1.drain_seconds([{"save": 0, "summary": None}]))

    def test_끝까지_안_비면_null(self):
        self.assertIsNone(collect_s1.drain_seconds([{"save": 3, "summary": 0}, {"save": 1, "summary": 0}]))
        self.assertIsNone(collect_s1.drain_seconds([]))

    def test_요약_큐가_없으면_빈_것으로_보되_저장_큐가_남으면_빈_것이_아니다(self):
        self.assertIsNone(collect_s1.drain_seconds([{"save": 5, "summary": None}]))
        self.assertEqual(5, collect_s1.drain_seconds([{"save": 5, "summary": None}, {"save": 0, "summary": None}]))

    def test_요약_큐만_남아도_빈_것이_아니다(self):
        self.assertEqual(10, collect_s1.drain_seconds([{"save": 0, "summary": 7}, {"save": 0, "summary": 2}, {"save": 0, "summary": 0}]))


class QueueDepthTest(unittest.TestCase):
    OUT = "chat.message.save.queue\t6799\nchat.message.save.dlq\t0\nother\t5\n"

    def test_rabbitmqctl_출력을_큐별_깊이로_푼다(self):
        d = collect_s1.parse_queue_depths(self.OUT)
        self.assertEqual({"chat.message.save.queue": 6799, "chat.message.save.dlq": 0, "other": 5}, d)

    def test_저장_요약_큐와_DLQ_네_값으로_고르고_없는_큐는_null(self):
        d = collect_s1.parse_queue_depths(self.OUT)
        self.assertEqual({"save": 6799, "summary": None, "save_dlq": 0, "summary_dlq": None}, collect_s1.depth_pair(d))


class ClockOffsetTest(unittest.TestCase):
    def test_DB_시각에서_호스트_왕복_중간_시각을_뺀_값이_오프셋(self):
        # 호스트 100.0 -> 100.2 사이에 DB 가 100.6 이라고 답했다면 중간 100.1 과 0.5초 차이
        self.assertEqual(500, collect_s1.clock_offset_ms(100.0, 100.2, 100.6))

    def test_psql_행을_밀리초_에포크로_바꾸고_호스트_UTC_오프셋을_뺀다(self):
        # timestamp(시간대 없음)를 UTC 로 읽은 에포크 초 -> 실제 호스트 시각은 KST(+32400) 만큼 앞서 읽힌 값
        text = "9001|%f|%f|f\n9002|%f||t\n" % (33400.5, 33405.25, 33407.75)
        rows = collect_s1.parse_rows(text, 32400)
        self.assertEqual({"qrid": 9001, "created_ms": 1000500.0, "updated_ms": 1005250.0, "summary_null": False}, rows[0])
        self.assertEqual({"qrid": 9002, "created_ms": 1007750.0, "updated_ms": None, "summary_null": True}, rows[1])


class SaveSummaryBlocksTest(unittest.TestCase):
    ROWS = [
        {"qrid": 1, "created_ms": 1500.0, "updated_ms": None, "summary_null": False},    # 요약과 함께 저장
        {"qrid": 2, "created_ms": 3000.0, "updated_ms": 7600.0, "summary_null": False},  # 요약이 4.6초 뒤 채워짐
        {"qrid": 3, "created_ms": 4000.0, "updated_ms": 4000.0, "summary_null": True},   # 요약 누락
    ]
    POSTS = {1: 1000.0, 2: 2000.0, 3: 3000.0, 4: 3500.0}  # 4번은 끝까지 행이 안 생김

    def test_저장_시간과_요약_지연과_누락_수가_정해진_키로_나온다(self):
        save, summary = collect_s1.save_summary_blocks(self.POSTS, self.ROWS, 0)
        self.assertEqual(3, save["rows"])
        self.assertEqual(4, save["posts"])
        self.assertEqual(1, save["not_durable"])
        self.assertEqual({"p50": 1000.0, "p95": 1000.0, "count": 3}, save["durable_ms"])
        self.assertEqual(2, summary["lag_ms"]["count"])
        self.assertEqual(1, summary["missing_at_end"])
        self.assertEqual(2, summary["missing_incl_unsaved"])

    def test_요약과_함께_저장된_행의_지연은_0(self):
        _, summary = collect_s1.save_summary_blocks(self.POSTS, self.ROWS, 0)
        self.assertEqual(0.0, summary["lag_ms"]["p50"])
        self.assertEqual(4600.0, summary["lag_ms"]["p95"])

    def test_측정한_DB_시계_오프셋만큼_행_시각을_보정한다(self):
        save, _ = collect_s1.save_summary_blocks({1: 1000.0}, self.ROWS[:1], 300)
        self.assertEqual({"p50": 200.0, "p95": 200.0, "count": 1}, save["durable_ms"])

    def test_행이_없으면_백분위는_null_개수는_0(self):
        save, summary = collect_s1.save_summary_blocks({1: 1000.0}, [], 0)
        self.assertEqual({"p50": None, "p95": None, "count": 0}, save["durable_ms"])
        self.assertEqual({"p50": None, "p95": None, "count": 0}, summary["lag_ms"])
        self.assertEqual(0, summary["missing_at_end"])
        self.assertEqual(1, save["not_durable"])


if __name__ == "__main__":
    unittest.main()
