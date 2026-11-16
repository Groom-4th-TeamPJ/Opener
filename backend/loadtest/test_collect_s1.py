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
        save, summary = collect_s1.save_summary_blocks(self.POSTS, self.ROWS)
        self.assertEqual(3, save["rows"])
        self.assertEqual(4, save["posts"])
        self.assertEqual(1, save["not_durable"])
        self.assertEqual({"p50": 1000.0, "p95": 1000.0, "count": 3}, save["durable_ms"])
        self.assertEqual(2, summary["lag_ms"]["count"])
        self.assertEqual(1, summary["missing_at_end"])
        self.assertEqual(2, summary["missing_incl_unsaved"])

    def test_요약과_함께_저장된_행의_지연은_0(self):
        _, summary = collect_s1.save_summary_blocks(self.POSTS, self.ROWS)
        self.assertEqual(0.0, summary["lag_ms"]["p50"])
        self.assertEqual(4600.0, summary["lag_ms"]["p95"])

    def test_저장_시간은_행_생성_시각에서_전송_시각을_뺀_값_그대로(self):
        # created_at 은 같은 호스트의 앱 JVM 이 쓰므로 DB 시계 오프셋을 빼지 않는다
        save, _ = collect_s1.save_summary_blocks({1: 1000.0}, self.ROWS[:1])
        self.assertEqual({"p50": 500.0, "p95": 500.0, "count": 1}, save["durable_ms"])

    def test_측정_구간_안에서_보낸_저장만_따로_p50_p95_max_개수로_센다(self):
        rows = [{"qrid": q, "created_ms": c, "updated_ms": None, "summary_null": False} for q, c in ((1, 1500.0), (2, 2400.0), (3, 9000.0))]
        save, _ = collect_s1.save_summary_blocks({1: 1000.0, 2: 2000.0, 3: 3000.0}, rows, hold_ms=(1800.0, 3500.0))
        self.assertEqual({"p50": 400.0, "p95": 6000.0, "max": 6000.0, "count": 2}, save["durable_ms_hold"])
        self.assertEqual(3, save["durable_ms"]["count"])

    def test_구간을_안_주면_구간_값은_null(self):
        save, _ = collect_s1.save_summary_blocks(self.POSTS, self.ROWS)
        self.assertIsNone(save["durable_ms_hold"])

    def test_요약과_함께_저장된_행도_측정한_updated_at_빼기_created_at_을_보고하고_주석을_남긴다(self):
        rows = [{"qrid": 1, "created_ms": 1000.0, "updated_ms": 1005.0, "summary_null": False},
                {"qrid": 2, "created_ms": 2000.0, "updated_ms": 6600.0, "summary_null": False}]
        _, summary = collect_s1.save_summary_blocks({1: 900.0, 2: 1900.0}, rows)
        self.assertEqual(5.0, summary["lag_ms"]["p50"])
        self.assertEqual(4600.0, summary["lag_ms"]["p95"])
        self.assertIn("updated_at", summary["lag_note"])

    def test_행이_없으면_백분위는_null_개수는_0(self):
        save, summary = collect_s1.save_summary_blocks({1: 1000.0}, [])
        self.assertEqual({"p50": None, "p95": None, "count": 0}, save["durable_ms"])
        self.assertEqual({"p50": None, "p95": None, "count": 0}, summary["lag_ms"])
        self.assertEqual(0, summary["missing_at_end"])
        self.assertEqual(1, save["not_durable"])


def prom_of(**metrics):
    """이름 -> 값 (라벨 없음) 형태의 프로메테우스 스냅샷"""
    return {(n, ()): v for n, v in metrics.items()}


class HikariBlockTest(unittest.TestCase):
    P0 = prom_of(hikaricp_connections_acquire_seconds_count=100.0, hikaricp_connections_acquire_seconds_sum=1.0,
                 hikaricp_connections_usage_seconds_count=100.0, hikaricp_connections_usage_seconds_sum=10.0,
                 hikaricp_connections_timeout_total=2.0)
    P1 = prom_of(hikaricp_connections_acquire_seconds_count=300.0, hikaricp_connections_acquire_seconds_sum=5.0,
                 hikaricp_connections_usage_seconds_count=300.0, hikaricp_connections_usage_seconds_sum=50.0,
                 hikaricp_connections_timeout_total=5.0)
    SAMPLES = [{"hikari_pending": 0.0, "hikari_acquire_max_s": 0.010, "hikari_usage_max_s": 0.5},
               {"hikari_pending": 7.0, "hikari_acquire_max_s": 0.250, "hikari_usage_max_s": 1.5},
               {"hikari_pending": 3.0, "hikari_acquire_max_s": 0.020, "hikari_usage_max_s": 0.7}]

    def test_대기_수_최댓값은_표본_중_가장_큰_값(self):
        # 첫 표본(0)이나 마지막 표본(3)이 아니라 가운데 7
        self.assertEqual(7, collect_s1.hikari_block(self.P0, self.P1, self.SAMPLES)["pending_peak"])

    def test_획득_사용_평균은_구간_합_증가분을_개수_증가분으로_나눈_밀리초(self):
        b = collect_s1.hikari_block(self.P0, self.P1, self.SAMPLES)
        self.assertAlmostEqual(20.0, b["acquire_ms"]["mean"])   # (5-1)/200 s
        self.assertAlmostEqual(200.0, b["usage_ms"]["mean"])    # (50-10)/200 s

    def test_획득_사용_최댓값은_표집한_max_게이지_중_가장_큰_값의_밀리초(self):
        b = collect_s1.hikari_block(self.P0, self.P1, self.SAMPLES)
        self.assertAlmostEqual(250.0, b["acquire_ms"]["max"])
        self.assertAlmostEqual(1500.0, b["usage_ms"]["max"])

    def test_타임아웃은_구간_증가분(self):
        self.assertEqual(3, collect_s1.hikari_block(self.P0, self.P1, self.SAMPLES)["timeouts"])

    def test_구간_안_개수가_안_늘었으면_평균은_null(self):
        b = collect_s1.hikari_block(self.P0, self.P0, [])
        self.assertIsNone(b["acquire_ms"]["mean"])
        self.assertIsNone(b["usage_ms"]["mean"])

    def test_표본이_없으면_대기_수와_최댓값은_null(self):
        b = collect_s1.hikari_block(self.P0, self.P1, [{"hikari_pending": None, "hikari_acquire_max_s": None, "hikari_usage_max_s": None}])
        self.assertIsNone(b["pending_peak"])
        self.assertIsNone(b["acquire_ms"]["max"])
        self.assertIsNone(b["usage_ms"]["max"])

    def test_지표가_아예_없으면_타임아웃도_null(self):
        self.assertIsNone(collect_s1.hikari_block({}, {}, [])["timeouts"])


class ActivityTest(unittest.TestCase):
    # psql -At 행 '상태|상태_경과초|질의문(한 줄)'
    TEXT = ("active|0.5|select v1_0.id from vector_store v1_0 order by v1_0.embedding <=> ?\n"
            "active|0.1|INSERT INTO chat_messages (a,b) values (?,?)\n"
            "active|0.2|update chat_messages set summary=? where id=?\n"
            "idle in transaction|3.5|select c1_0.id from chat_messages c1_0 where c1_0.id=?\n"
            "idle in transaction|9.25|select 1\n"
            "active|0.3|select * from users where id=?\n")

    def test_질의문_규칙으로_종류를_나누고_대소문자는_가리지_않는다(self):
        self.assertEqual("vector", collect_s1.query_class("SELECT * FROM Vector_Store"))
        self.assertEqual("chat_insert", collect_s1.query_class("INSERT INTO chat_messages (a) values (?)"))
        self.assertEqual("chat_update", collect_s1.query_class("update chat_messages set x=1"))
        self.assertEqual("chat_select", collect_s1.query_class("select * from chat_messages where id=1"))
        self.assertEqual("other", collect_s1.query_class("select * from users"))

    def test_여러_규칙에_맞으면_먼저_나열한_규칙을_따른다(self):
        # vector_store 가 chat_messages 보다 앞
        self.assertEqual("vector", collect_s1.query_class("select m.id from chat_messages m join vector_store v on true"))
        # insert 는 from chat_messages 보다 앞
        self.assertEqual("chat_insert", collect_s1.query_class("insert into chat_messages select * from chat_messages"))

    def test_표본_한_번을_상태_x_종류_개수로_푼다(self):
        s = collect_s1.parse_activity(self.TEXT)
        self.assertEqual({"vector": 1, "chat_insert": 1, "chat_update": 1, "chat_select": 0, "other": 1}, s["active"])
        self.assertEqual({"vector": 0, "chat_insert": 0, "chat_update": 0, "chat_select": 1, "other": 1}, s["idle_in_transaction"])

    def test_트랜잭션_안에서_논_시간은_그_상태_연결_중_가장_긴_값(self):
        self.assertEqual(9.25, collect_s1.parse_activity(self.TEXT)["idle_in_tx_max_s"])

    def test_출력이_비면_모든_개수가_0이고_논_시간은_null(self):
        s = collect_s1.parse_activity("")
        self.assertEqual(0, sum(s["active"].values()))
        self.assertEqual(0, sum(s["idle_in_transaction"].values()))
        self.assertIsNone(s["idle_in_tx_max_s"])

    def test_상태가_둘_밖이면_세지_않는다(self):
        s = collect_s1.parse_activity("idle|1.0|select 1\nfastpath function call|0.1|select 2\n")
        self.assertEqual(0, sum(s["active"].values()) + sum(s["idle_in_transaction"].values()))

    def test_구간_값은_상태_x_종류별_표본_최댓값과_합계_최댓값(self):
        a = collect_s1.parse_activity("active|0|select from vector_store\nactive|0|select from vector_store\nactive|0|select 1\n")
        b = collect_s1.parse_activity("active|0|select from vector_store\nactive|0|insert into chat_messages x\n"
                                      "idle in transaction|4.0|select 1\n")
        blk = collect_s1.activity_block([a, None, b])
        self.assertEqual(2, blk["peak"]["active"]["vector"])
        self.assertEqual(1, blk["peak"]["active"]["chat_insert"])
        self.assertEqual(1, blk["peak"]["active"]["other"])
        self.assertEqual(3, blk["peak_total"]["active"])     # 표본 a 한 번에 3개
        self.assertEqual(1, blk["peak_total"]["idle_in_transaction"])
        self.assertEqual(4.0, blk["idle_in_tx_max_s"])
        self.assertEqual(2, blk["samples"])                  # None(읽기 실패) 은 뺀다
        self.assertEqual(1, blk["samples_failed"])

    def test_읽은_표본이_없으면_구간_값은_null(self):
        blk = collect_s1.activity_block([None, None])
        self.assertEqual(0, blk["samples"])
        self.assertIsNone(blk["peak"])
        self.assertIsNone(blk["idle_in_tx_max_s"])


class RawBlocksTest(unittest.TestCase):
    def setUp(self):
        import json
        import tempfile
        self.json = json
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)
        self.out = self.dir.name
        # 행 둘 - 1번은 요약 있음, 2번은 요약 누락
        json.dump({"rows_raw": "1|2.0|2.003|f\n2|3.0|3.0|t\n", "host_gmtoff_s": 0, "clock_offset_ms": 12},
                  open(os.path.join(self.out, "drain.json"), "w"))
        self.sessions = [{"save_code": 200, "save_sent_ms": 1500.0, "question_result_id": 1},
                         {"save_code": 200, "save_sent_ms": 2500.0, "question_result_id": 2},
                         {"save_code": 500, "save_sent_ms": 2600.0, "question_result_id": 3},   # 실패 응답은 제외
                         {"save_code": 200, "save_sent_ms": 9000.0, "question_result_id": 4}]  # 행 없음

    def test_저장_요약_블록을_원자료로_만들고_실패한_저장_요청은_뺀다(self):
        _, save, summary = collect_s1.raw_blocks(self.out, self.sessions, (1.0, 3.0))
        self.assertEqual(3, save["posts"])
        self.assertEqual(2, save["rows"])
        self.assertEqual(1, save["not_durable"])
        self.assertEqual(12, save["clock_offset_ms"])
        self.assertEqual(1, summary["missing_at_end"])
        self.assertEqual(2, summary["missing_incl_unsaved"])

    def test_측정_구간은_초_단위_인자를_밀리초로_바꿔_쓴다(self):
        _, save, _ = collect_s1.raw_blocks(self.out, self.sessions, (1.0, 2.0))
        self.assertEqual(1, save["durable_ms_hold"]["count"])   # 보낸 시각 1500 만 [1000, 2000] 안
        self.assertEqual(500.0, save["durable_ms_hold"]["p50"])

    def test_같은_결과_id_저장이_둘이면_먼저_보낸_것만_센다(self):
        sess = [{"save_code": 200, "save_sent_ms": 1800.0, "question_result_id": 1},
                {"save_code": 200, "save_sent_ms": 1500.0, "question_result_id": 1}]
        _, save, _ = collect_s1.raw_blocks(self.out, sess, (1.0, 3.0))
        self.assertEqual(1, save["posts"])
        self.assertEqual(500.0, save["durable_ms"]["p50"])

    def test_drain_json_이_없으면_행_0_오프셋_null(self):
        os.remove(os.path.join(self.out, "drain.json"))
        dr, save, summary = collect_s1.raw_blocks(self.out, self.sessions, (1.0, 3.0))
        self.assertIsNone(dr)
        self.assertEqual(0, save["rows"])
        self.assertIsNone(save["clock_offset_ms"])
        self.assertEqual(3, summary["missing_incl_unsaved"])


class ResummarizeTest(unittest.TestCase):
    def setUp(self):
        import json
        import tempfile
        self.json = json
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)
        self.out = self.dir.name
        self.old = {"code": "AB", "ttft_ms": {"p50": 1}, "save": {"rows": 0}, "summary": {"queue_published": 5, "queue_consumed": 4, "lag_ms": None}}
        json.dump(self.old, open(os.path.join(self.out, "summary.json"), "w"))
        json.dump({"hold_start": 1.0, "hold_end": 3.0}, open(os.path.join(self.out, "window.json"), "w"))
        json.dump({"rows_raw": "1|2.0|2.003|f\n", "host_gmtoff_s": 0, "clock_offset_ms": 12}, open(os.path.join(self.out, "drain.json"), "w"))
        with open(os.path.join(self.out, "sessions-0.jsonl"), "w") as f:
            f.write(json.dumps({"save_code": 200, "save_sent_ms": 1500.0, "question_result_id": 1}) + "\n")

    def read(self, name):
        return self.json.load(open(os.path.join(self.out, name)))

    def test_저장_요약_블록만_다시_쓰고_나머지는_그대로_둔다(self):
        collect_s1.resummarize(self.out)
        new = self.read("summary.json")
        self.assertEqual(1, new["save"]["rows"])
        self.assertEqual(500.0, new["save"]["durable_ms"]["p50"])
        self.assertEqual("AB", new["code"])
        self.assertEqual({"p50": 1}, new["ttft_ms"])

    def test_큐_발행_소비_수는_옛_값을_가져온다(self):
        collect_s1.resummarize(self.out)
        s = self.read("summary.json")["summary"]
        self.assertEqual(5, s["queue_published"])
        self.assertEqual(4, s["queue_consumed"])
        self.assertEqual(1, s["lag_ms"]["count"])

    def test_옛_파일은_v1_로_남기고_두_번째_실행이_덮어쓰지_않는다(self):
        collect_s1.resummarize(self.out)
        self.assertEqual(self.old, self.read("summary.v1.json"))
        collect_s1.resummarize(self.out)
        self.assertEqual(self.old, self.read("summary.v1.json"))


if __name__ == "__main__":
    unittest.main()
