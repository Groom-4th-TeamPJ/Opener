"""수집기 순수 함수 시험 - commandstats 파싱·스크립트 내부 명령 제외·분위수·프로세스 CPU 시간·턴 통계"""
import json
import os
import subprocess
import sys
import tempfile
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


def write_json(path, obj):
    with open(path, "w") as f:
        json.dump(obj, f)


def read_json(path):
    with open(path) as f:
        return json.load(f)


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


class EmbeddingBlockTest(unittest.TestCase):
    OK = 'application="backend",error="none",gen_ai_operation_name="embedding",gen_ai_request_model="m",gen_ai_system="openai"'
    ERR = 'application="backend",error="WebClientRequestException",gen_ai_operation_name="embedding",gen_ai_request_model="m",gen_ai_system="openai"'
    CHAT = 'application="backend",error="none",gen_ai_operation_name="chat",gen_ai_request_model="m",gen_ai_system="openai"'

    def prom(self, ok_count, ok_sum, err_count=0, chat_count=500, chat_sum=900.0):
        text = (f"gen_ai_client_operation_seconds_count{{{self.OK}}} {ok_count}\ngen_ai_client_operation_seconds_sum{{{self.OK}}} {ok_sum}\n"
                f"gen_ai_client_operation_seconds_count{{{self.ERR}}} {err_count}\ngen_ai_client_operation_seconds_sum{{{self.ERR}}} 0.5\n"
                f"gen_ai_client_operation_seconds_count{{{self.CHAT}}} {chat_count}\ngen_ai_client_operation_seconds_sum{{{self.CHAT}}} {chat_sum}\n")
        return collect_s1.parse_prom(text)

    def test_평균은_구간_합_증가분을_개수_증가분으로_나눈_밀리초이고_채팅_호출은_섞지_않는다(self):
        b = collect_s1.embedding_block(self.prom(100, 15.0), self.prom(300, 75.0, chat_count=9999, chat_sum=1e6), [])
        self.assertEqual(200, b["count"])
        self.assertAlmostEqual(300.0, b["mean_ms"])    # (75-15)/200 s

    def test_최댓값은_표집한_embed_max_s_중_가장_큰_값의_밀리초(self):
        b = collect_s1.embedding_block(self.prom(1, 1.0), self.prom(2, 2.0), [{"embed_max_s": 0.4}, {"embed_max_s": 6.85}, {"embed_max_s": None}])
        self.assertAlmostEqual(6850.0, b["max_ms"])

    def test_오류로_끝난_임베딩_호출은_평균에_넣지_않고_개수만_센다(self):
        b = collect_s1.embedding_block(self.prom(10, 1.0, err_count=1), self.prom(20, 3.0, err_count=5), [])
        self.assertEqual(10, b["count"])
        self.assertAlmostEqual(200.0, b["mean_ms"])
        self.assertEqual(4, b["failed_count"])

    def test_지표가_없으면_모두_null(self):
        b = collect_s1.embedding_block({}, {}, [])
        self.assertEqual({"count": None, "mean_ms": None, "max_ms": None, "failed_count": None}, b)


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
        import tempfile
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)
        self.out = self.dir.name
        # 행 둘 - 1번은 요약 있음, 2번은 요약 누락
        write_json(os.path.join(self.out, "drain.json"), {"rows_raw": "1|2.0|2.003|f\n2|3.0|3.0|t\n", "host_gmtoff_s": 0, "clock_offset_ms": 12})
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
        import tempfile
        self.dir = tempfile.TemporaryDirectory()
        self.addCleanup(self.dir.cleanup)
        self.out = self.dir.name
        self.old = {"code": "AB", "ttft_ms": {"p50": 1}, "save": {"rows": 0}, "summary": {"queue_published": 5, "queue_consumed": 4, "lag_ms": None}}
        write_json(os.path.join(self.out, "summary.json"), self.old)
        write_json(os.path.join(self.out, "window.json"), {"hold_start": 1.0, "hold_end": 3.0})
        write_json(os.path.join(self.out, "drain.json"), {"rows_raw": "1|2.0|2.003|f\n", "host_gmtoff_s": 0, "clock_offset_ms": 12})
        with open(os.path.join(self.out, "sessions-0.jsonl"), "w") as f:
            f.write(json.dumps({"save_code": 200, "save_sent_ms": 1500.0, "question_result_id": 1}) + "\n")

    def read(self, name):
        return read_json(os.path.join(self.out, name))

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


def breaker_text(name, state, not_permitted, failed, successful=100):
    """resilience4j 지표 한 개 차단기분의 프로메테우스 본문 - state 게이지는 상태마다 한 줄, 현재 상태만 1"""
    lines = [f'resilience4j_circuitbreaker_state{{application="backend",name="{name}",state="{st}"}} {1.0 if st == state.lower() else 0.0}'
             for st in ("closed", "open", "half_open", "disabled", "forced_open", "metrics_only")]
    lines += [
        f'resilience4j_circuitbreaker_not_permitted_calls_total{{application="backend",kind="not_permitted",name="{name}"}} {float(not_permitted)}',
        f'resilience4j_circuitbreaker_calls_seconds_count{{application="backend",kind="failed",name="{name}"}} {failed}',
        f'resilience4j_circuitbreaker_calls_seconds_count{{application="backend",kind="successful",name="{name}"}} {successful}',
    ]
    return "\n".join(lines) + "\n"


def circuit_sample_at(t, **by_name):
    """by_name: 차단기 이름 -> (상태, 허용 안 된 호출, 실패 호출), 이름의 밑줄은 하이픈으로 바꾼다"""
    return {"t": t, "breakers": {n.replace("_", "-"): {"state": v[0], "not_permitted": v[1], "failed": v[2]} for n, v in by_name.items()}}


class CircuitSampleTest(unittest.TestCase):
    def test_circuit_표본은_차단기_넷의_상태_허용_안_된_호출_실패_호출을_읽는다(self):
        text = (breaker_text("llm-chat", "OPEN", 12, 30) + breaker_text("llm-rag", "HALF_OPEN", 1, 4)
                + breaker_text("llm-summary", "CLOSED", 0, 0) + breaker_text("redis-chat", "OPEN", 99, 99))
        row = collect_s1.circuit_sample(collect_s1.parse_prom(text), 123.5)
        self.assertEqual(123.5, row["t"])
        self.assertEqual({"state": "OPEN", "not_permitted": 12, "failed": 30}, row["breakers"]["llm-chat"])
        self.assertEqual("HALF_OPEN", row["breakers"]["llm-rag"]["state"])
        self.assertEqual("CLOSED", row["breakers"]["llm-summary"]["state"])
        self.assertEqual({"state": "OPEN", "not_permitted": 99, "failed": 99}, row["breakers"]["redis-chat"])

    def test_circuit_성공_호출은_실패_호출로_세지_않는다(self):
        row = collect_s1.circuit_sample(collect_s1.parse_prom(breaker_text("llm-chat", "CLOSED", 0, 3, successful=500)), 1.0)
        self.assertEqual(3, row["breakers"]["llm-chat"]["failed"])

    def test_circuit_지표가_없는_차단기는_표본에서_뺀다(self):
        row = collect_s1.circuit_sample(collect_s1.parse_prom(breaker_text("llm-chat", "CLOSED", 0, 0)), 1.0)
        self.assertEqual(["llm-chat"], list(row["breakers"]))

    def test_circuit_본문을_못_읽으면_표본은_None(self):
        self.assertIsNone(collect_s1.circuit_sample({}, 1.0))


class CircuitBlockTest(unittest.TestCase):
    FAULT = {"start_ms": 100_000, "end_ms": 160_000}

    def block(self, samples):
        return collect_s1.circuit_block(self.FAULT, samples)

    def test_circuit_열림까지_시간은_장애_시작에서_처음_OPEN_표본까지(self):
        b = self.block([circuit_sample_at(95, llm_chat=("CLOSED", 0, 0)), circuit_sample_at(100, llm_chat=("CLOSED", 0, 2)),
                        circuit_sample_at(103, llm_chat=("OPEN", 0, 9)), circuit_sample_at(104, llm_chat=("OPEN", 5, 9))])
        self.assertEqual(3.0, b["llm-chat"]["open_after_fault_s"])

    def test_circuit_장애_시작_전에_OPEN_이던_표본은_열림으로_세지_않는다(self):
        b = self.block([circuit_sample_at(90, llm_chat=("OPEN", 0, 0)), circuit_sample_at(101, llm_chat=("CLOSED", 0, 0))])
        self.assertIsNone(b["llm-chat"]["open_after_fault_s"])

    def test_circuit_닫힘까지_시간은_장애_끝에서_다시_CLOSED_가_된_표본까지(self):
        # 장애 시작(100) 기준이면 80, 끝(160) 기준이면 20
        b = self.block([circuit_sample_at(103, llm_chat=("OPEN", 0, 9)), circuit_sample_at(170, llm_chat=("OPEN", 50, 9)),
                        circuit_sample_at(178, llm_chat=("HALF_OPEN", 50, 9)), circuit_sample_at(180, llm_chat=("CLOSED", 50, 9))])
        self.assertEqual(20.0, b["llm-chat"]["closed_after_fault_end_s"])

    def test_circuit_장애_끝_뒤에도_열려_있고_마지막_표본까지_안_닫히면_null(self):
        b = self.block([circuit_sample_at(103, llm_chat=("OPEN", 0, 9)), circuit_sample_at(300, llm_chat=("OPEN", 50, 9))])
        self.assertEqual(3.0, b["llm-chat"]["open_after_fault_s"])
        self.assertIsNone(b["llm-chat"]["closed_after_fault_end_s"])

    def test_circuit_한_번도_안_열렸으면_열림도_닫힘도_null(self):
        b = self.block([circuit_sample_at(t, llm_chat=("CLOSED", 0, t - 90)) for t in (95, 120, 170)])
        self.assertIsNone(b["llm-chat"]["open_after_fault_s"])
        self.assertIsNone(b["llm-chat"]["closed_after_fault_end_s"])

    def test_circuit_허용_안_된_호출과_실패_호출은_첫_표본에서_마지막_표본까지의_증가분(self):
        b = self.block([circuit_sample_at(90, llm_chat=("CLOSED", 5, 10)), circuit_sample_at(130, llm_chat=("OPEN", 20, 40)),
                        circuit_sample_at(200, llm_chat=("CLOSED", 45, 70))])
        self.assertEqual(40, b["llm-chat"]["not_permitted"])
        self.assertEqual(60, b["llm-chat"]["failed_calls"])

    def test_circuit_차단기마다_따로_계산한다(self):
        b = self.block([circuit_sample_at(95, llm_chat=("CLOSED", 0, 0), llm_rag=("CLOSED", 0, 0), llm_summary=("CLOSED", 0, 0)),
                        circuit_sample_at(110, llm_chat=("CLOSED", 0, 3), llm_rag=("OPEN", 0, 20), llm_summary=("CLOSED", 0, 0)),
                        circuit_sample_at(165, llm_chat=("CLOSED", 0, 3), llm_rag=("CLOSED", 7, 20), llm_summary=("CLOSED", 0, 0))])
        self.assertEqual(10.0, b["llm-rag"]["open_after_fault_s"])
        self.assertEqual(5.0, b["llm-rag"]["closed_after_fault_end_s"])
        self.assertIsNone(b["llm-chat"]["open_after_fault_s"])
        self.assertIsNone(b["llm-summary"]["open_after_fault_s"])

    def test_circuit_상태가_바뀐_시점을_장애_시작_기준_초로_남긴다(self):
        b = self.block([circuit_sample_at(95, llm_chat=("CLOSED", 0, 0)), circuit_sample_at(103, llm_chat=("OPEN", 0, 9)),
                        circuit_sample_at(104, llm_chat=("OPEN", 1, 9)), circuit_sample_at(175, llm_chat=("HALF_OPEN", 9, 9))])
        self.assertEqual([[-5.0, "CLOSED"], [3.0, "OPEN"], [75.0, "HALF_OPEN"]], b["llm-chat"]["transitions"])

    def test_circuit_표본이_없으면_차단기_넷_모두_null(self):
        b = self.block([])
        self.assertEqual({"llm-chat", "llm-rag", "llm-summary", "redis-chat"}, set(b))
        self.assertIsNone(b["llm-chat"]["open_after_fault_s"])
        self.assertIsNone(b["llm-chat"]["not_permitted"])


class FaultTurnsTest(unittest.TestCase):
    @staticmethod
    def turn(t_start, ok=True, error=None, turn_ms=9000.0, ttft_ms=1500.0):
        return {"t_start": t_start, "ok": ok, "error": error, "turn_ms": turn_ms, "ttft_ms": ttft_ms if ok else None}

    def test_fault_턴은_장애_시작부터_장애_끝_60초_뒤까지_시작한_턴만_센다(self):
        turns = [self.turn(99.9), self.turn(100.0), self.turn(130.0), self.turn(220.0), self.turn(220.1)]
        t = collect_s1.fault_turns(turns, 100_000, 160_000)
        self.assertEqual(3, t["attempted"])

    def test_fault_성공_실패_수와_실패_종류별_수(self):
        turns = [self.turn(110), self.turn(111, ok=False, error="sse_error_event", turn_ms=800.0),
                 self.turn(112, ok=False, error="sse_error_event", turn_ms=900.0), self.turn(113, ok=False, error="turn_timeout", turn_ms=60000.0)]
        t = collect_s1.fault_turns(turns, 100_000, 160_000)
        self.assertEqual((4, 1, 3), (t["attempted"], t["ok"], t["failed"]))
        self.assertEqual({"sse_error_event": 2, "turn_timeout": 1}, t["by_kind"])

    def test_fault_실패한_턴_시간은_턴_시작에서_오류까지의_p50_p95(self):
        turns = [self.turn(110 + i, ok=False, error="sse_error_event", turn_ms=float(ms)) for i, ms in enumerate((100, 200, 300, 400, 5000))]
        t = collect_s1.fault_turns(turns, 100_000, 160_000)
        self.assertEqual(300.0, t["failed_turn_ms"]["p50"])
        self.assertEqual(5000.0, t["failed_turn_ms"]["p95"])

    def test_fault_성공한_턴의_첫_토큰_p95는_성공_턴만으로_구한다(self):
        turns = [self.turn(110, ttft_ms=1000.0), self.turn(111, ttft_ms=2000.0), self.turn(112, ok=False, error="sse_error_event", turn_ms=50.0)]
        self.assertEqual(2000.0, collect_s1.fault_turns(turns, 100_000, 160_000)["ok_ttft_p95_ms"])

    def test_fault_구간에_턴이_없으면_0과_null(self):
        t = collect_s1.fault_turns([self.turn(10)], 100_000, 160_000)
        self.assertEqual((0, 0, 0), (t["attempted"], t["ok"], t["failed"]))
        self.assertEqual({}, t["by_kind"])
        self.assertIsNone(t["failed_turn_ms"]["p50"])
        self.assertIsNone(t["ok_ttft_p95_ms"])


class FaultBlockTest(unittest.TestCase):
    def test_fault_블록은_구간과_모의_서버의_경로별_실패_수와_턴_집계를_담는다(self):
        mock = {"fault": {"start_ms": 100_000, "end_ms": 160_000, "failed_by_route": {"chat_stream": 4, "chat_nonstream": 2, "embeddings": 9}}}
        f = collect_s1.fault_block({"start_ms": 100_000, "end_ms": 160_000}, mock, [FaultTurnsTest.turn(110)])
        self.assertEqual(100_000, f["start_ms"])
        self.assertEqual(160_000, f["end_ms"])
        self.assertEqual({"chat_stream": 4, "chat_nonstream": 2, "embeddings": 9}, f["failed_by_route"])
        self.assertEqual(1, f["turns"]["attempted"])

    def test_fault_모의_서버_통계를_못_읽으면_실패_수는_null(self):
        f = collect_s1.fault_block({"start_ms": 100_000, "end_ms": 160_000}, None, [])
        self.assertIsNone(f["failed_by_route"])
        self.assertEqual(100_000, f["start_ms"])


class ThreadsBlockR129Test(unittest.TestCase):
    HOLD = (100.0, 160.0)

    @staticmethod
    def sample(t, live, busy=None):
        return {"t": t, "jvm_threads_live": live, "tomcat_busy": busy}

    @staticmethod
    def turn(t_start, ttft_ms, turn_ms, ok=True):
        return {"t_start": t_start, "ttft_ms": ttft_ms, "turn_ms": turn_ms, "ok": ok, "t_end": t_start + turn_ms / 1000}

    def block(self, samples=(), turns=()):
        return collect_s1.threads_block(list(samples), list(turns), self.HOLD)

    def test_r129_플랫폼_스레드_최댓값과_중앙값은_표본의_jvm_threads_live_에서_구한다(self):
        b = self.block([self.sample(100 + i, v) for i, v in enumerate((120, 140, 130, 500, 150))])
        self.assertEqual(500, b["platform_live_max"])
        self.assertEqual(140, b["platform_live_median"])

    def test_r129_값이_없는_표본은_건너뛰고_표본이_없으면_null(self):
        b = self.block([self.sample(100, None), self.sample(101, 90)])
        self.assertEqual((90, 90), (b["platform_live_max"], b["platform_live_median"]))
        empty = self.block()
        self.assertIsNone(empty["platform_live_max"])
        self.assertIsNone(empty["tomcat_busy_max"])

    def test_r129_톰캣_바쁜_스레드_최댓값_지표가_없으면_null(self):
        self.assertEqual(7, self.block([self.sample(100, 90, 3), self.sample(101, 90, 7)])["tomcat_busy_max"])
        self.assertIsNone(self.block([self.sample(100, 90, None)])["tomcat_busy_max"])

    def test_r129_톰캣_지표가_음수면_지표_없음으로_보고_null(self):
        # 가상 스레드에서는 톰캣 풀 지표가 -1 로 나온다 (tiny 실측)
        self.assertIsNone(self.block([self.sample(100, 90, -1), self.sample(101, 90, -1)])["tomcat_busy_max"])
        self.assertEqual(4, self.block([self.sample(100, 90, -1), self.sample(101, 90, 4)])["tomcat_busy_max"])

    def test_r129_동시_스트림은_첫_토큰부터_끝까지를_초_단위로_겹쳐_센다(self):
        # 스트림 구간 = t_start + ttft -> t_end: A 110~118, B 112~120, C 115.5~116.5 -> 115초 칸에 셋
        turns = [self.turn(108.0, 2000, 10000), self.turn(110.0, 2000, 10000), self.turn(114.0, 1500, 2500)]
        self.assertEqual(3, self.block(turns=turns)["streams_concurrent_max"])

    def test_r129_끝과_시작이_맞닿은_스트림은_같은_초에_겹치지_않는다(self):
        turns = [self.turn(100.0, 1000, 3000), self.turn(103.0, 1000, 3000)]  # 101~103, 104~106
        self.assertEqual(1, self.block(turns=turns)["streams_concurrent_max"])
        touching = [self.turn(100.0, 1000, 4000), self.turn(102.0, 1000, 2000)]  # 101~104 와 103~104 는 겹침
        self.assertEqual(2, self.block(turns=touching)["streams_concurrent_max"])
        exact = [self.turn(100.0, 0, 2000), self.turn(102.0, 0, 2000)]  # 100~102, 102~104 는 한 초 칸에 하나씩
        self.assertEqual(1, self.block(turns=exact)["streams_concurrent_max"])

    def test_r129_첫_토큰이_없는_턴과_측정_구간_밖_스트림은_세지_않는다(self):
        turns = [self.turn(110.0, None, 800, ok=False), self.turn(10.0, 1000, 8000), self.turn(110.0, 1000, 8000)]
        self.assertEqual(1, self.block(turns=turns)["streams_concurrent_max"])
        self.assertEqual(0, self.block()["streams_concurrent_max"])

    def test_r129_스트림_도중_실패한_턴도_첫_토큰이_있으면_실패_시각까지_센다(self):
        turns = [self.turn(110.0, 1000, 5000, ok=False), self.turn(112.0, 1000, 5000)]
        self.assertEqual(2, self.block(turns=turns)["streams_concurrent_max"])


class CircuitRedisChatR129Test(unittest.TestCase):
    FAULT = {"start_ms": 100_000, "end_ms": 160_000}

    def test_r129_redis_chat_차단기도_llm_과_같은_필드를_가진다(self):
        b = collect_s1.circuit_block(self.FAULT, [
            circuit_sample_at(95, redis_chat=("CLOSED", 0, 0)), circuit_sample_at(104, redis_chat=("OPEN", 0, 12)),
            circuit_sample_at(120, redis_chat=("OPEN", 300, 12)), circuit_sample_at(172, redis_chat=("CLOSED", 410, 12))])
        r = b["redis-chat"]
        self.assertEqual({"open_after_fault_s", "closed_after_fault_end_s", "not_permitted", "failed_calls", "transitions"}, set(r))
        self.assertEqual((4.0, 12.0, 410, 12), (r["open_after_fault_s"], r["closed_after_fault_end_s"], r["not_permitted"], r["failed_calls"]))
        self.assertEqual([[-5.0, "CLOSED"], [4.0, "OPEN"], [72.0, "CLOSED"]], r["transitions"])
        self.assertIsNone(b["llm-chat"]["open_after_fault_s"])

    def test_r129_redis_chat_는_llm_chat_과_따로_센다(self):
        b = collect_s1.circuit_block(self.FAULT, [
            circuit_sample_at(95, llm_chat=("CLOSED", 0, 0), redis_chat=("CLOSED", 0, 0)),
            circuit_sample_at(110, llm_chat=("CLOSED", 0, 0), redis_chat=("OPEN", 5, 20))])
        self.assertEqual(10.0, b["redis-chat"]["open_after_fault_s"])
        self.assertIsNone(b["llm-chat"]["open_after_fault_s"])


def node_line(node_id, name, flags, master_id="-", slots="", ip="172.31.0.9", port=27380):
    return f"{node_id} {ip}:{port}@3{port},{name} {flags} {master_id} 0 1794979982241 2 connected {slots}".rstrip()


def cluster_text(*lines):
    return "\n".join(lines) + "\n"


def cluster_sample(t, *lines):
    return {"t": t, "nodes": collect_s1.parse_cluster_nodes(cluster_text(*lines))}


class ClusterNodesR129Test(unittest.TestCase):
    def test_r129_cluster_nodes_줄을_이름_플래그_마스터_슬롯으로_푼다(self):
        nodes = collect_s1.parse_cluster_nodes(cluster_text(
            node_line("a1", "redis-chat-1", "myself,master", slots="0-5460 7000"),
            node_line("b1", "redis-chat-5", "slave", master_id="a1"),
            node_line("c1", "redis-chat-2", "master,fail", slots="5461-6000")))
        self.assertEqual(3, len(nodes))
        n1 = nodes[0]
        self.assertEqual(("a1", "redis-chat-1", ["master", "myself"], None), (n1["id"], n1["name"], sorted(n1["flags"]), n1["master_id"]))
        self.assertEqual([[0, 5460], [7000, 7000]], n1["slots"])
        self.assertEqual("a1", nodes[1]["master_id"])
        self.assertEqual([], nodes[1]["slots"])
        self.assertIn("fail", nodes[2]["flags"])

    def test_r129_호스트명이_없는_주소도_읽는다(self):
        nodes = collect_s1.parse_cluster_nodes("zz 10.0.0.1:7000@17000 master - 0 1 1 connected 0-10\n")
        self.assertEqual("10.0.0.1:7000", nodes[0]["name"])


class FailoverBlockR129Test(unittest.TestCase):
    FAULT = {"start_ms": 100_000, "end_ms": 160_000, "kind": "master-stop", "stopped_node": "redis-chat-2"}

    BEFORE = (node_line("m2", "redis-chat-2", "master", slots="5461-10922"),
              node_line("r6", "redis-chat-6", "slave", master_id="m2"))

    def block(self, samples):
        return collect_s1.failover_block(self.FAULT, samples)

    def test_r129_승격_시간은_장애_시작에서_복제본이_그_슬롯을_마스터로_가진_표본까지(self):
        b = self.block([
            cluster_sample(95, *self.BEFORE),
            cluster_sample(101, node_line("m2", "redis-chat-2", "master,fail?", slots="5461-10922"), node_line("r6", "redis-chat-6", "slave", master_id="m2")),
            cluster_sample(107, node_line("m2", "redis-chat-2", "master,fail", slots="5461-10922"), node_line("r6", "redis-chat-6", "master", slots="5461-10922"))])
        self.assertEqual("redis-chat-2", b["stopped_node"])
        self.assertEqual(7.0, b["promoted_after_s"])
        self.assertEqual("redis-chat-6", b["promoted_node"])

    def test_r129_fail_표시가_붙은_옛_마스터는_승격으로_세지_않는다(self):
        b = self.block([cluster_sample(95, *self.BEFORE),
                        cluster_sample(110, node_line("m2", "redis-chat-2", "master,fail", slots="5461-10922"), node_line("r6", "redis-chat-6", "slave", master_id="m2"))])
        self.assertIsNone(b["promoted_after_s"])
        self.assertIsNone(b["promoted_node"])
        self.assertFalse(b["rejoined_as_replica"])

    def test_r129_멈춘_노드가_시작_뒤_승격한_노드의_복제본으로_돌아오면_rejoined_as_replica(self):
        b = self.block([
            cluster_sample(95, *self.BEFORE),
            cluster_sample(106, node_line("m2", "redis-chat-2", "master,fail", slots="5461-10922"), node_line("r6", "redis-chat-6", "master", slots="5461-10922")),
            cluster_sample(158, node_line("m2", "redis-chat-2", "master,fail", slots="5461-10922"), node_line("r6", "redis-chat-6", "master", slots="5461-10922")),
            cluster_sample(171, node_line("m2", "redis-chat-2", "slave", master_id="r6"), node_line("r6", "redis-chat-6", "master", slots="5461-10922"))])
        self.assertTrue(b["rejoined_as_replica"])
        self.assertEqual(11.0, b["rejoined_after_s"])

    def test_r129_장애_끝_전에_replica_로_보인_표본은_돌아온_것으로_세지_않는다(self):
        b = self.block([
            cluster_sample(95, *self.BEFORE),
            cluster_sample(106, node_line("m2", "redis-chat-2", "slave", master_id="r6"), node_line("r6", "redis-chat-6", "master", slots="5461-10922"))])
        self.assertFalse(b["rejoined_as_replica"])
        self.assertIsNone(b["rejoined_after_s"])

    def test_r129_표본이_없으면_모두_null_또는_False(self):
        b = self.block([])
        self.assertEqual(("redis-chat-2", None, False), (b["stopped_node"], b["promoted_after_s"], b["rejoined_as_replica"]))


class FaultBlockR129Test(unittest.TestCase):
    def test_r129_redis_장애_구간의_턴을_센다(self):
        turns = [FaultTurnsTest.turn(110), FaultTurnsTest.turn(111, ok=False, error="sse_error_event", turn_ms=300.0),
                 FaultTurnsTest.turn(112, ok=False, error="conn_error", turn_ms=900.0), FaultTurnsTest.turn(400)]
        f = collect_s1.fault_block({"start_ms": 100_000, "end_ms": 160_000, "kind": "master-stop", "stopped_node": "redis-chat-2"}, None, turns)
        self.assertEqual("master-stop", f["kind"])
        t = f["turns"]
        self.assertEqual((3, 1, 2), (t["attempted"], t["ok"], t["failed"]))
        self.assertEqual({"sse_error_event": 1, "conn_error": 1}, t["by_kind"])
        self.assertEqual(900.0, t["failed_turn_ms"]["p95"])
        self.assertIsNone(f["failed_by_route"])

    def test_r129_멈춤_시간은_기록한_값이_있으면_그_값_없으면_끝_빼기_시작(self):
        f = collect_s1.fault_block({"start_ms": 100_000, "end_ms": 160_400, "kind": "cluster-pause", "paused_ms": 60_123}, None, [])
        self.assertEqual(60_123, f["paused_ms"])
        g = collect_s1.fault_block({"start_ms": 100_000, "end_ms": 160_400, "kind": "cluster-pause"}, None, [])
        self.assertEqual(60_400, g["paused_ms"])

    def test_r129_종류를_안_적은_장애는_mock_이고_멈춤_시간을_안_적는다(self):
        f = collect_s1.fault_block({"start_ms": 100_000, "end_ms": 160_000}, None, [])
        self.assertEqual("mock", f["kind"])
        self.assertNotIn("paused_ms", f)

    def test_r129_장애_구간의_모의_서버_요청_수는_끝_빼기_시작(self):
        a = {"chat_streams": 100, "chat_nonstream": 10, "embeddings": 50, "prompt_missing": 0}
        b = {"chat_streams": 130, "chat_nonstream": 10, "embeddings": 90, "prompt_missing": 0}
        self.assertEqual({"chat_streams": 30, "chat_nonstream": 0, "embeddings": 40}, collect_s1.mock_delta(a, b))
        self.assertIsNone(collect_s1.mock_delta(None, b))


class FaultEmbeddingsR130Test(unittest.TestCase):
    def test_r130_mock_embeddings_종류가_장애_종류에_있다(self):
        self.assertIn("mock-embeddings", collect_s1.FAULT_KINDS)

    def test_r130_mock_embeddings_는_임베딩_경로만_구간에_건다(self):
        body = collect_s1.mock_fault_body("mock-embeddings", 100_000, 160_000)
        self.assertEqual({"start_ms": 100_000, "end_ms": 160_000, "routes": ["embeddings"]}, body)

    def test_r130_mock_은_경로를_안_정해_세_경로가_모두_실패한다(self):
        self.assertEqual({"start_ms": 100_000, "end_ms": 160_000}, collect_s1.mock_fault_body("mock", 100_000, 160_000))

    def test_r130_fault_블록은_mock_embeddings_종류를_그대로_적는다(self):
        f = collect_s1.fault_block({"start_ms": 100_000, "end_ms": 160_000, "kind": "mock-embeddings"}, None, [])
        self.assertEqual("mock-embeddings", f["kind"])


class RagFallbackR130Test(unittest.TestCase):
    def prom(self, chat, opener):
        rows = {}
        if chat is not None:
            rows[("rag_vector_search_fallback_total", (("path", "chat"),))] = chat
        if opener is not None:
            rows[("rag_vector_search_fallback_total", (("path", "opener"),))] = opener
        return rows

    def test_r130_rag_fallback_은_경로별_구간_증가분(self):
        self.assertEqual({"chat": 40, "opener": 3}, collect_s1.rag_fallback(self.prom(2.0, 1.0), self.prom(42.0, 4.0)))

    def test_r130_카운터가_없으면_한_번도_안_늘어난_0(self):
        # Micrometer 카운터는 처음 증가할 때 등록된다
        self.assertEqual({"chat": 0, "opener": 0}, collect_s1.rag_fallback({}, {}))

    def test_r130_한쪽_경로만_있으면_다른_쪽은_0(self):
        self.assertEqual({"chat": 7, "opener": 0}, collect_s1.rag_fallback({}, self.prom(7.0, None)))


class FailedTurnShareR130Test(unittest.TestCase):
    @staticmethod
    def turn(ok, in_window=True, **extra):
        return {"vu": 0, "session": 1, "turn": 1, "t_start": 1.0, "t_end": 2.0, "ttft_ms": 1.0, "turn_ms": 1000.0,
                "ok": ok, "error": None if ok else "sse_error_event", "in_window": in_window, **extra}

    def test_r130_실패_턴을_측정_구간에_끝난_턴_시도_수로_나눈다(self):
        turns = [self.turn(True), self.turn(True), self.turn(False), self.turn(False), self.turn(False, in_window=False), self.turn(True, in_window=False)]
        self.assertEqual({"attempted": 4, "failed": 2, "share": 0.5}, collect_s1.failed_turn_share(turns))

    def test_r130_구간에_끝난_턴이_없으면_share_는_null(self):
        self.assertEqual({"attempted": 0, "failed": 0, "share": None}, collect_s1.failed_turn_share([self.turn(False, in_window=False)]))

    def test_r130_옛_도구의_줄에_error_detail_이_없어도_같은_함수로_읽는다(self):
        old = {"vu": 4, "session": 42, "turn": 1, "t_start": 1.0, "t_end": 2.0, "ttft_ms": 5.0, "turn_ms": 9.0, "ok": False,
               "error": "turn_timeout", "in_window": True}
        new = self.turn(False, error_detail="오류 본문")
        self.assertNotIn("error_detail", old)
        self.assertEqual(2, collect_s1.failed_turn_share([old, new])["failed"])

    def test_r130_폴더_목록을_읽어_폴더_이름별_JSON_한_개를_쓴다(self):
        with tempfile.TemporaryDirectory() as tmp:
            for name, rows in (("s1-a", [self.turn(True), self.turn(False)]), ("s1-b", [self.turn(True)])):
                d = os.path.join(tmp, name)
                os.makedirs(d)
                with open(os.path.join(d, "turns-0.jsonl"), "w") as f:
                    f.write("\n".join(json.dumps(r) for r in rows) + "\n")
                with open(os.path.join(d, "turns-1.jsonl"), "w") as f:
                    f.write(json.dumps(self.turn(False)) + "\n" if name == "s1-a" else "")
            out = os.path.join(tmp, "share.json")
            r = subprocess.run([sys.executable, os.path.join(HERE, "collect_s1.py"), "share", "--share-out", out,
                                os.path.join(tmp, "s1-a"), os.path.join(tmp, "s1-b")], capture_output=True, text=True)
            self.assertEqual(0, r.returncode, r.stderr)
            with open(out) as f:
                got = json.load(f)
        self.assertEqual({"s1-a": {"attempted": 3, "failed": 2, "share": 2 / 3}, "s1-b": {"attempted": 1, "failed": 0, "share": 0.0}}, got)


if __name__ == "__main__":
    unittest.main()
