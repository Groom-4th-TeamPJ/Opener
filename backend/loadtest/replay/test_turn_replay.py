"""turn_replay 시험 - RESP 부호화, 슬롯 계산, 턴 명령 열(전·후), 저장 스크립트, 포화 판정"""
import hashlib
import json
import unittest

import turn_replay as tr


def drive(gen, handler):
    """생성기가 내놓는 명령을 handler 로 응답하며 끝까지 돌려 (명령 목록, 반환값) 을 돌려준다"""
    log = []
    try:
        cmd = next(gen)
        while True:
            log.append(cmd)
            cmd = gen.send(handler(cmd))
    except StopIteration as stop:
        return log, stop.value


class FakeCluster:
    """한 세션만 다루는 메모리 저장소 - replica_lag 개만큼 replica 의 목록 끝이 모자란다"""

    def __init__(self, prior=0, replica_lag=0, owner=b"u-1"):
        self.messages = [b"m%d" % i for i in range(prior)]
        self.replica_lag = replica_lag
        self.owner = owner

    @staticmethod
    def _slice(items, start, end):
        start = int(start)
        end = int(end)
        if start < 0:
            start = max(0, len(items) + start)
        end = len(items) + end if end < 0 else end
        return items[start:end + 1]

    def handle(self, cmd):
        name = cmd.args[0]
        if name == b"HGET":
            return self.owner
        if name == b"RPUSH":
            self.messages.append(cmd.args[2])
            return len(self.messages)
        if name == b"EXPIRE":
            return 1
        if name == b"EVALSHA":
            self.messages.append(cmd.args[5])
            return len(self.messages)
        if name == b"LRANGE":
            items = self.messages
            if cmd.role == "replica" and self.replica_lag:
                items = items[:len(items) - self.replica_lag]
            return self._slice(items, cmd.args[2], cmd.args[3])
        raise AssertionError("뜻밖의 명령 %r" % (cmd,))


def roles(log):
    return [c.role for c in log]


def names(log):
    return [c.args[0] for c in log]


class RespCodecTest(unittest.TestCase):
    def test_encode_문자열_정수_바이트를_배열로_부호화(self):
        self.assertEqual(
            tr.encode_command([b"SET", "k", 7]),
            b"*3\r\n$3\r\nSET\r\n$1\r\nk\r\n$1\r\n7\r\n")

    def test_encode_본문의_CRLF_는_길이로_감싼다(self):
        self.assertEqual(tr.encode_command([b"A", b"x\r\ny"]),
                         b"*2\r\n$1\r\nA\r\n$4\r\nx\r\ny\r\n")

    def test_decode_단순_문자열_오류_정수(self):
        self.assertEqual(tr.decode_reply(b"+OK\r\n"), (b"OK", 5))
        self.assertEqual(tr.decode_reply(b":42\r\n"), (42, 5))
        value, pos = tr.decode_reply(b"-ERR boom\r\n")
        self.assertIsInstance(value, tr.RespError)
        self.assertEqual(str(value), "ERR boom")
        self.assertEqual(pos, 11)

    def test_decode_벌크_널_배열(self):
        self.assertEqual(tr.decode_reply(b"$3\r\nabc\r\n"), (b"abc", 9))
        self.assertEqual(tr.decode_reply(b"$-1\r\n"), (None, 5))
        self.assertEqual(tr.decode_reply(b"*3\r\n$1\r\na\r\n$-1\r\n:5\r\n"),
                         ([b"a", None, 5], 20))
        self.assertEqual(tr.decode_reply(b"*0\r\n"), ([], 4))

    def test_decode_벌크_안의_CRLF_는_본문으로_읽는다(self):
        self.assertEqual(tr.decode_reply(b"$4\r\nx\r\ny\r\n"), (b"x\r\ny", 10))

    def test_decode_덜_온_응답은_None(self):
        for partial in (b"", b"+OK", b"$5\r\nab", b"$5\r\nabcde\r", b"*2\r\n$1\r\na\r\n", b":1"):
            self.assertIsNone(tr.decode_reply(partial), partial)

    def test_decode_pos_부터_이어_읽는다(self):
        buf = b":1\r\n:2\r\n"
        first, pos = tr.decode_reply(buf)
        self.assertEqual(tr.decode_reply(buf, pos), (2, 8))
        self.assertEqual(first, 1)

    def test_decode_bytearray_도_읽는다(self):
        self.assertEqual(tr.decode_reply(bytearray(b"*1\r\n$2\r\nhi\r\n")), ([b"hi"], 12))


class SlotTest(unittest.TestCase):
    def test_crc16_표준_검사값(self):
        self.assertEqual(tr.crc16(b"123456789"), 0x31C3)

    def test_key_slot_알려진_값(self):
        self.assertEqual(tr.key_slot("foo"), 12182)
        # 2026-08-13 CQRS 실측 문서가 적은 값 - {7} 은 슬롯 1716
        self.assertEqual(tr.key_slot("chat:{7}:messages"), 1716)
        self.assertEqual(tr.key_slot("chat:{7}:session"), 1716)

    def test_key_slot_해시태그는_안쪽만_본다(self):
        self.assertEqual(tr.key_slot("a{b}c"), tr.key_slot("{b}"))
        self.assertEqual(tr.key_slot("{}x"), tr.crc16(b"{}x") % 16384)

    def test_세션_두_키는_같은_슬롯(self):
        for sid in (1, 500, 30000):
            self.assertEqual(tr.key_slot(tr.message_key(sid)), tr.key_slot(tr.session_key(sid)))


class MessageTest(unittest.TestCase):
    def test_질문_150바이트_답_2400바이트(self):
        self.assertEqual(len(tr.QUESTION), 150)
        self.assertEqual(len(tr.ANSWER), 2400)

    def test_메시지는_JSON_이고_무작위가_아니다(self):
        for raw, role in ((tr.QUESTION, "USER"), (tr.ANSWER, "ASSISTANT")):
            obj = json.loads(raw)
            self.assertEqual(obj["chatRole"], role)
            self.assertIs(obj["partial"], False)
        self.assertEqual(tr.make_message("USER", 150), tr.QUESTION)


class BeforeSequenceTest(unittest.TestCase):
    def test_한_턴은_8명령_master_7_replica_1(self):
        log, info = drive(tr.turn_before(7), FakeCluster(prior=4).handle)
        self.assertEqual(len(log), 8)
        self.assertEqual(roles(log), ["replica"] + ["master"] * 7)
        self.assertEqual(names(log), [b"HGET", b"RPUSH", b"EXPIRE", b"EXPIRE",
                                      b"LRANGE", b"RPUSH", b"EXPIRE", b"EXPIRE"])
        self.assertEqual(info, {"owner_recheck": False, "fallback": False})

    def test_명령_인자가_방법값과_같다(self):
        log, _ = drive(tr.turn_before(7), FakeCluster().handle)
        m, s = b"chat:{7}:messages", b"chat:{7}:session"
        self.assertEqual([c.args for c in log], [
            [b"HGET", s, b"userId"],
            [b"RPUSH", m, tr.QUESTION],
            [b"EXPIRE", m, b"3600"],
            [b"EXPIRE", s, b"3600"],
            [b"LRANGE", m, b"-20", b"-1"],
            [b"RPUSH", m, tr.ANSWER],
            [b"EXPIRE", m, b"3600"],
            [b"EXPIRE", s, b"3600"],
        ])

    def test_replica_가_소유자를_못_주면_master_재확인을_덧붙인다(self):
        cluster = FakeCluster(owner=None)
        log, info = drive(tr.turn_before(7), cluster.handle)
        self.assertEqual(len(log), 9)
        self.assertEqual(roles(log)[:2], ["replica", "master"])
        self.assertEqual(names(log)[:2], [b"HGET", b"HGET"])
        self.assertTrue(info["owner_recheck"])


class AfterSequenceTest(unittest.TestCase):
    def test_둘째_턴_이후는_4명령_master_2_replica_2(self):
        log, info = drive(tr.turn_after(7), FakeCluster(prior=4).handle)
        self.assertEqual(len(log), 4)
        self.assertEqual(roles(log), ["replica", "master", "replica", "master"])
        self.assertEqual(names(log), [b"HGET", b"EVALSHA", b"LRANGE", b"EVALSHA"])
        self.assertEqual(info, {"owner_recheck": False, "fallback": False})

    def test_첫_턴은_길이가_1이라_이력_읽기를_건너뛴다(self):
        log, _ = drive(tr.turn_after(7), FakeCluster(prior=0).handle)
        self.assertEqual(roles(log), ["replica", "master", "master"])
        self.assertEqual(names(log), [b"HGET", b"EVALSHA", b"EVALSHA"])

    def test_저장은_EVALSHA_로_키_2개_본문_TTL(self):
        log, _ = drive(tr.turn_after(7), FakeCluster(prior=4).handle)
        m, s = b"chat:{7}:messages", b"chat:{7}:session"
        sha = hashlib.sha1(tr.SAVE_SCRIPT.encode()).hexdigest().encode()
        self.assertEqual(log[1].args, [b"EVALSHA", sha, b"2", m, s, tr.QUESTION, b"3600"])
        self.assertEqual(log[3].args, [b"EVALSHA", sha, b"2", m, s, tr.ANSWER, b"3600"])
        self.assertEqual(tr.SAVE_SHA, sha.decode())

    def test_이력은_저장이_돌려준_길이_L_로_절대_인덱스를_읽는다(self):
        # 앞선 4건 + 질문 = L 5 -> [0, L-2] = 0..3
        log, _ = drive(tr.turn_after(7), FakeCluster(prior=4).handle)
        self.assertEqual(log[2].args, [b"LRANGE", b"chat:{7}:messages", b"0", b"3"])

    def test_이력_창은_20건으로_자른다(self):
        # 앞선 30건 + 질문 = L 31 -> from 11, to 29
        log, _ = drive(tr.turn_after(7), FakeCluster(prior=30).handle)
        self.assertEqual(log[2].args, [b"LRANGE", b"chat:{7}:messages", b"11", b"29"])

    def test_이력_읽기는_replica_로_간다(self):
        log, _ = drive(tr.turn_after(7), FakeCluster(prior=4).handle)
        self.assertEqual(log[2].role, "replica")

    def test_replica_가_모자라게_주면_master_로_같은_범위를_다시_읽고_센다(self):
        log, info = drive(tr.turn_after(7), FakeCluster(prior=4, replica_lag=2).handle)
        self.assertEqual(roles(log), ["replica", "master", "replica", "master", "master"])
        self.assertEqual(names(log), [b"HGET", b"EVALSHA", b"LRANGE", b"LRANGE", b"EVALSHA"])
        self.assertEqual(log[3].args, log[2].args)
        self.assertTrue(info["fallback"])

    def test_replica_가_딱_맞게_주면_되읽기가_없다(self):
        log, info = drive(tr.turn_after(7), FakeCluster(prior=4, replica_lag=0).handle)
        self.assertFalse(info["fallback"])
        self.assertEqual(names(log).count(b"LRANGE"), 1)

    def test_replica_가_소유자를_못_주면_master_재확인을_덧붙인다(self):
        log, info = drive(tr.turn_after(7), FakeCluster(prior=4, owner=None).handle)
        self.assertEqual(roles(log)[:2], ["replica", "master"])
        self.assertEqual(names(log)[:2], [b"HGET", b"HGET"])
        self.assertTrue(info["owner_recheck"])


class SaveScriptTest(unittest.TestCase):
    def test_저장_스크립트는_방법값의_네_문장과_같다(self):
        self.assertEqual(tr.SAVE_SCRIPT.strip().splitlines(), [
            "local n = redis.call('RPUSH', KEYS[1], ARGV[1])",
            "redis.call('EXPIRE', KEYS[1], ARGV[2])",
            "redis.call('EXPIRE', KEYS[2], ARGV[2])",
            "return n",
        ])


class SessionResetTest(unittest.TestCase):
    def test_세션_정리는_두_키_UNLINK_와_선점으로_master_3명령(self):
        log, _ = drive(tr.session_reset(7, "owner-x", 1700000000000), lambda cmd: 1)
        self.assertEqual(roles(log), ["master"] * 3)
        self.assertEqual([c.args[:2] for c in log[:2]],
                         [[b"UNLINK", b"chat:{7}:messages"], [b"UNLINK", b"chat:{7}:session"]])
        claim = log[2].args
        self.assertEqual(claim[0], b"EVAL")
        self.assertEqual(claim[1], tr.CLAIM_SCRIPT.encode())
        self.assertEqual(claim[2:], [b"1", b"chat:{7}:session", b"owner-x", b"3600", b"1700000000000"])

    def test_선점_스크립트는_앱의_파일과_같다(self):
        import os
        path = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources",
                            "scripts", "chat-session-claim.lua")
        if not os.path.exists(path):
            self.skipTest("앱 저장소 밖에서 실행")
        with open(path, encoding="utf-8") as f:
            self.assertEqual(tr.CLAIM_SCRIPT, f.read())


class SaturationTest(unittest.TestCase):
    """R125-14 - 포화는 처리량 정체만으로 판정, CPU 비율은 판정에 쓰지 않는다"""

    @staticmethod
    def steps(*pairs):
        return [{"turns_per_s": t, "master_cpu_pct": c} for t, c in pairs]

    def test_마스터_CPU_가_높아도_처리량이_늘면_포화_아님(self):
        s = self.steps((1000, 95), (2000, 96), (3900, 97))
        self.assertIsNone(tr.saturation_index(s))

    def test_처리량_증가가_5퍼센트_미만이면_그_단계가_포화(self):
        s = self.steps((1000, 20), (2000, 50), (3000, 70), (3100, 80))
        self.assertEqual(tr.saturation_index(s), 3)

    def test_정확히_5퍼센트_늘면_포화_아님(self):
        s = self.steps((1000, 20), (1050, 50))
        self.assertIsNone(tr.saturation_index(s))

    def test_처리량이_줄어도_포화(self):
        s = self.steps((1000, 20), (2000, 50), (1900, 60))
        self.assertEqual(tr.saturation_index(s), 2)

    def test_추가_단계는_마지막_두_단계가_5퍼센트_이상_늘었을_때만(self):
        self.assertTrue(tr.need_extra_step(self.steps((1000, 1), (1100, 1))))
        self.assertFalse(tr.need_extra_step(self.steps((1000, 1), (1049, 1))))
        self.assertFalse(tr.need_extra_step(self.steps((1000, 1))))

    def test_단계_목록은_8에서_1024까지_추가_2048(self):
        self.assertEqual(tr.STEPS, (8, 16, 32, 64, 128, 256, 512, 1024))
        self.assertEqual(tr.EXTRA_STEP, 2048)


class PercentileTest(unittest.TestCase):
    def test_p95_는_가까운_순위(self):
        self.assertEqual(tr.percentile(list(range(1, 101)), 95), 95)
        self.assertEqual(tr.percentile([5], 95), 5)
        self.assertEqual(tr.percentile([], 95), 0.0)


INFO_TEXT = """# Memory
used_memory:1000
# CPU
used_cpu_sys:1.5
used_cpu_user:2.25
used_cpu_sys_children:0.1
used_cpu_user_children:0.2
# Commandstats
cmdstat_rpush:calls=10,usec=30,usec_per_call=3.00,rejected_calls=0,failed_calls=0
cmdstat_info:calls=2,usec=100,usec_per_call=50.00,rejected_calls=0,failed_calls=0
"""


class InfoParseTest(unittest.TestCase):
    def test_parse_info_CPU_는_자식_프로세스를_빼고_sys_user_합(self):
        got = tr.parse_info(INFO_TEXT)
        self.assertAlmostEqual(got["cpu"], 3.75)
        self.assertEqual(got["mem"], 1000)

    def test_parse_info_명령별_호출수와_usec(self):
        got = tr.parse_info(INFO_TEXT)
        self.assertEqual(got["cmd"], {"rpush": (10, 30.0), "info": (2, 100.0)})


class NodeDeltaTest(unittest.TestCase):
    @staticmethod
    def snap(t, cpu, cmd, mem=0):
        return {"t": t, "cpu": cpu, "mem": mem, "cmd": cmd}

    def test_CPU_와_명령_증가분과_CPU_퍼센트(self):
        a = self.snap(10.0, 1.0, {"rpush": (10, 30.0)})
        b = self.snap(12.0, 2.0, {"rpush": (110, 330.0), "expire": (5, 5.0)}, mem=7)
        d = tr.node_delta(a, b)
        self.assertAlmostEqual(d["cpu_s"], 1.0)
        self.assertAlmostEqual(d["wall_s"], 2.0)
        self.assertAlmostEqual(d["cpu_pct"], 50.0)
        self.assertEqual(d["cmd"], {"rpush": {"calls": 100, "usec": 300.0},
                                    "expire": {"calls": 5, "usec": 5.0}})
        self.assertEqual(d["mem"], 7)

    def test_늘지_않은_명령은_빠진다(self):
        a = self.snap(0.0, 0.0, {"rpush": (10, 30.0)})
        b = self.snap(1.0, 0.1, {"rpush": (10, 30.0)})
        self.assertEqual(tr.node_delta(a, b)["cmd"], {})

    def test_카운터가_초기화되면_음수가_아니라_새_값을_증가분으로(self):
        a = self.snap(0.0, 5.0, {"rpush": (10, 30.0)})
        b = self.snap(1.0, 0.5, {"rpush": (4, 12.0)})
        d = tr.node_delta(a, b)
        self.assertEqual(d["cmd"], {"rpush": {"calls": 4, "usec": 12.0}})
        self.assertAlmostEqual(d["cpu_s"], 0.5)


class CapacityTest(unittest.TestCase):
    def test_capacity_는_1e6_을_턴당_usec_로_나눈_값(self):
        self.assertEqual(tr.capacity(100.0), 10000.0)
        self.assertEqual(tr.capacity(0), 0.0)


class SplitCmdUsecTest(unittest.TestCase):
    """commandstats usec 를 채팅 턴 몫과 세션 부담(UNLINK·선점 스크립트) 몫으로 가른다"""

    @staticmethod
    def cs(**kw):
        return {k: {"calls": c, "usec": u} for k, (c, u) in kw.items()}

    def test_master_before(self):
        cs = self.cs(hget=(1, 2.0), rpush=(20, 60.0), expire=(42, 84.0), lrange=(10, 30.0),
                     eval=(10, 150.0), unlink=(20, 28.0), hsetnx=(10, 18.0))
        chat, overhead = tr.split_cmd_usec("master", "before", cs, 10)
        # expire 42 호출 중 10 호출은 선점 스크립트 안쪽 -> 32/42 만 턴 몫
        self.assertAlmostEqual(chat, 2.0 + 60.0 + 64.0 + 30.0)
        self.assertAlmostEqual(overhead, 150.0 + 28.0)

    def test_master_after_저장_스크립트_안쪽_명령은_EVALSHA_에_포함(self):
        cs = self.cs(evalsha=(20, 300.0), rpush=(20, 70.0), expire=(42, 40.0), lrange=(1, 3.0),
                     hget=(5, 5.0), eval=(10, 80.0), unlink=(20, 40.0))
        chat, overhead = tr.split_cmd_usec("master", "after", cs, 10)
        self.assertAlmostEqual(chat, 300.0 + 3.0 + 5.0)
        self.assertAlmostEqual(overhead, 80.0 + 40.0)

    def test_replica_before(self):
        cs = self.cs(hget=(10, 20.0), rpush=(20, 60.0), pexpireat=(42, 42.0), unlink=(20, 30.0),
                     exec=(10, 30.0), multi=(10, 1.0), hsetnx=(10, 10.0), hset=(10, 6.0))
        chat, overhead = tr.split_cmd_usec("replica", "before", cs, 10)
        self.assertAlmostEqual(chat, 20.0 + 60.0 + 32.0)
        self.assertAlmostEqual(overhead, 30.0 + 30.0 + 1.0)

    def test_replica_after_EXEC_안에_복제된_쓰기가_들어_있다(self):
        cs = self.cs(hget=(10, 20.0), lrange=(8, 20.0), exec=(30, 90.0), multi=(30, 3.0),
                     rpush=(20, 40.0), pexpireat=(42, 17.0), unlink=(20, 30.0), hsetnx=(10, 10.0))
        chat, overhead = tr.split_cmd_usec("replica", "after", cs, 10)
        self.assertAlmostEqual(chat, 20.0 + 20.0 + 93.0 * 20 / 30)
        self.assertAlmostEqual(overhead, 30.0 + 93.0 * 10 / 30)

    def test_세션_정리가_없으면_부담은_0(self):
        cs = self.cs(hget=(1, 2.0), rpush=(2, 6.0), expire=(4, 8.0), lrange=(1, 3.0))
        chat, overhead = tr.split_cmd_usec("master", "before", cs, 0)
        self.assertAlmostEqual(chat, 19.0)
        self.assertEqual(overhead, 0.0)


def mk_step(clients, tps, **over):
    """finish_pass · summarize_sequence 가 읽는 키만 채운 작은 단계 기록"""
    st = {
        "clients": clients, "procs": 8, "turns_per_s": float(tps), "cpu_quota": 0.3,
        "master_cpu_pct": 95.0, "replica_cpu_pct": 50.0, "master_cpu_pct_raw": 28.5,
        "master_cpu_pct_by_node": [95.0, 94.0, 96.0], "replica_cpu_pct_by_node": [50.0, 50.0, 50.0],
        "master_round_trips_per_turn": 7.0, "replica_round_trips_per_turn": 1.0,
        "turn_p95_ms": 10.0, "generator_cpu_pct": 200.0,
        "master_cpu_us_per_turn": 100.0, "replica_cpu_us_per_turn": 50.0,
        "master_cmd_usec_per_turn_chat_only": 30.0, "replica_cmd_usec_per_turn_chat_only": 15.0,
        "master_turns_per_s_by_node": [tps / 3.0] * 3,
        "history_reads": 0, "fallbacks": 0, "fallback_rate": 0.0,
    }
    st.update(over)
    return st


def mk_pass(seq, tps_list, us_list, **over):
    clients = (8, 16, 32, 64, 128, 256, 512, 1024)
    steps = [mk_step(clients[i], t, master_cpu_us_per_turn=u) for i, (t, u) in enumerate(zip(tps_list, us_list))]
    for st in steps:
        st.update(over)
    return tr.finish_pass(seq, steps, cores=10)


class FinishPassTest(unittest.TestCase):
    def test_정체_단계와_최대_처리량_단계를_따로_고른다(self):
        p = mk_pass("before", [1000, 1800, 2500, 2600, 2700, 2650], [200, 150, 120, 110, 100, 105])
        self.assertEqual(p["saturation_step"], 3)
        self.assertTrue(p["saturated"])
        self.assertEqual(p["max_turns_per_s"], 2700)
        self.assertEqual(p["max_step_clients"], 128)

    def test_턴당_CPU_는_최대_처리량_단계_값_첫_단계_값은_따로(self):
        p = mk_pass("before", [1000, 1800, 2500, 2600, 2700, 2650], [200, 150, 120, 110, 100, 105])
        self.assertEqual(p["master_cpu_us_per_turn"], 100)
        self.assertEqual(p["master_cpu_us_per_turn_at_first_step"], 200)
        self.assertEqual(p["replica_cpu_us_per_turn_at_first_step"], 50)
        self.assertAlmostEqual(p["master_capacity_turns_per_s_per_core"], 10000.0)
        self.assertNotIn("master_cpu_us_per_turn_at_max", p)

    def test_계속_늘면_포화_아님이고_최대는_마지막_단계(self):
        p = mk_pass("after", [1000, 2000, 4000], [100, 80, 60])
        self.assertFalse(p["saturated"])
        self.assertIsNone(p["saturation_step"])
        self.assertEqual(p["max_step_clients"], 32)
        self.assertEqual(p["master_cpu_us_per_turn"], 60)

    def test_마지막_두_단계가_정체면_앞에서_이미_정체였어도_포화(self):
        p = mk_pass("after", [1000, 1010, 3000, 3050], [100, 100, 80, 80])
        self.assertEqual(p["saturation_step"], 1)
        self.assertTrue(p["saturated"])

    def test_마스터_세_대의_쏠림은_최대_처리량_단계의_노드별_턴으로(self):
        steps = [mk_step(8, 1000), mk_step(16, 1020, master_turns_per_s_by_node=[400.0, 330.0, 290.0])]
        p = tr.finish_pass("before", steps, cores=10)
        self.assertEqual(p["master_turns_per_s_by_node"], [400.0, 330.0, 290.0])
        self.assertEqual(p["busiest_master_turns_per_s"], 400.0)
        self.assertAlmostEqual(p["master_unevenness_pct"], (400 - 290) / (1020 / 3) * 100)


class LimitedByTest(unittest.TestCase):
    def test_마스터가_할당의_90퍼센트_이상이면_redis(self):
        self.assertEqual(tr.derive_limited_by(mk_step(256, 1000, master_cpu_pct=90.0), 10), "redis")

    def test_생성기가_쓸_수_있는_코어를_거의_다_쓰면_generator(self):
        st = mk_step(256, 1000, master_cpu_pct=40.0, generator_cpu_pct=750.0,
                     master_cpu_pct_by_node=[40.0] * 3, replica_cpu_pct_by_node=[20.0] * 3)
        self.assertEqual(tr.derive_limited_by(st, 10), "generator")

    def test_둘_다_아니면_unknown(self):
        st = mk_step(256, 1000, master_cpu_pct=40.0, generator_cpu_pct=200.0,
                     master_cpu_pct_by_node=[40.0] * 3, replica_cpu_pct_by_node=[20.0] * 3)
        self.assertEqual(tr.derive_limited_by(st, 10), "unknown")

    def test_Redis_가_쓰는_코어만큼_생성기가_쓸_코어가_줄어든다(self):
        # 코어 10 · 생성기 프로세스 8 · Redis 3.5 코어 사용 -> 생성기 몫 6.5 코어, 5.2 코어 사용 = 0.8
        st = mk_step(256, 1000, master_cpu_pct=73.0, cpu_quota=1.0, generator_cpu_pct=520.0,
                     master_cpu_pct_by_node=[73.0] * 3, replica_cpu_pct_by_node=[43.0] * 3)
        self.assertEqual(tr.derive_limited_by(st, 10), "generator")

    def test_할당_정보가_없는_옛_기록은_한_코어_기준으로_읽는다(self):
        st = mk_step(256, 1000, master_cpu_pct=73.0, generator_cpu_pct=520.0,
                     master_cpu_pct_by_node=[73.0] * 3, replica_cpu_pct_by_node=[43.0] * 3)
        del st["cpu_quota"]
        self.assertEqual(tr.derive_limited_by(st, 10), "generator")

    def test_pass_의_limited_by_는_최대_처리량_단계에서_유도한다(self):
        p = mk_pass("before", [1000, 1800, 2500], [200, 150, 120])
        self.assertEqual(p["limited_by"], "redis")
        self.assertIn("90", p["limited_by_rule"])


class SummarizeTest(unittest.TestCase):
    def setUp(self):
        self.b1 = mk_pass("before", [1000, 1800, 2500, 2600, 2700], [200, 150, 120, 110, 100],
                          master_round_trips_per_turn=7.0, replica_round_trips_per_turn=1.0)
        self.b2 = mk_pass("before", [1000, 1800, 2500, 2600, 2900], [200, 150, 120, 110, 110],
                          master_round_trips_per_turn=7.0, replica_round_trips_per_turn=1.0)
        extra = dict(master_round_trips_per_turn=2.0, replica_round_trips_per_turn=1.8,
                     history_reads=1000, fallbacks=2, fallback_rate=0.002)
        self.a1 = mk_pass("after", [2000, 3000, 3900, 4000, 4050], [60, 55, 50, 50, 50], **extra)
        self.a2 = mk_pass("after", [2000, 3000, 3900, 4000, 4150], [60, 55, 50, 50, 50], **extra)

    def test_steps_는_두_회차를_pass_번호로_이어붙인다(self):
        s = tr.summarize_sequence([self.b1, self.b2])
        self.assertEqual(len(s["steps"]), 10)
        self.assertEqual([st["pass"] for st in s["steps"]], [1] * 5 + [2] * 5)

    def test_최대_처리량은_회차_평균_회차별_값과_편차를_함께(self):
        s = tr.summarize_sequence([self.b1, self.b2])
        self.assertAlmostEqual(s["max_turns_per_s"], 2800.0)
        self.assertEqual(s["max_turns_per_s_by_pass"], [2700, 2900])
        self.assertAlmostEqual(s["max_turns_per_s_spread_pct"], 200 / 2800 * 100)

    def test_턴당_CPU_는_최대_처리량_단계_기준_평균(self):
        s = tr.summarize_sequence([self.b1, self.b2])
        self.assertAlmostEqual(s["master_cpu_us_per_turn"], 105.0)
        self.assertEqual(s["master_cpu_us_per_turn_by_pass"], [100, 110])
        self.assertAlmostEqual(s["master_cpu_us_per_turn_at_first_step"], 200.0)
        self.assertAlmostEqual(s["master_capacity_turns_per_s_per_core"], 1e6 / 105.0)
        self.assertNotIn("master_cpu_us_per_turn_at_max", s)

    def test_replica_몫은_왕복과_CPU_양쪽(self):
        s = tr.summarize_sequence([self.a1, self.a2])
        self.assertAlmostEqual(s["replica_round_trip_share"], 1.8 / 3.8)
        self.assertAlmostEqual(s["replica_cpu_share"], 50.0 / (50.0 + 50.0))

    def test_fallback_률은_전체_읽기_대비(self):
        s = tr.summarize_sequence([self.a1, self.a2])
        self.assertAlmostEqual(s["fallback_rate"], (2 * 5 * 2) / (1000 * 5 * 2))
        self.assertEqual(s["fallbacks"], 20)
        self.assertAlmostEqual(s["fallback_rate_at_max_step"], 0.002)

    def test_limited_by_는_회차마다_유도한_값(self):
        s = tr.summarize_sequence([self.b1, self.b2])
        self.assertEqual(s["limited_by_by_pass"], ["redis", "redis"])

    def test_build_summary_after_before_비와_unthrottled_유도값(self):
        slow = mk_step(256, 26000, master_cpu_pct=73.0, cpu_quota=1.0, generator_cpu_pct=520.0,
                       master_cpu_pct_by_node=[73.0] * 3, replica_cpu_pct_by_node=[43.0] * 3)
        raw = {"sequence": "before", "steps": [mk_step(8, 6000), slow], "env": {"cpu_count": 10}}
        out = tr.build_summary([self.b1, self.b2], [self.a1, self.a2], [raw])
        r = out["ratio_new_over_base"]
        self.assertAlmostEqual(r["max_turns_per_s"], 4100.0 / 2800.0)
        self.assertAlmostEqual(r["master_cpu_us_per_turn"], 50.0 / 105.0)
        self.assertAlmostEqual(r["master_cmd_usec_per_turn_chat_only"], 1.0)
        self.assertEqual(out["base_unthrottled"]["limited_by_by_pass"], ["generator"])
        self.assertEqual(out["base_unthrottled"]["max_turns_per_s"], 26000)
        self.assertIn("fallback_rate", out["new"])
        self.assertTrue(out["base"]["saturated"] and out["new"]["saturated"])



if __name__ == "__main__":
    unittest.main()
