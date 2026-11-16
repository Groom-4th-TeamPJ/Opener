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
    @staticmethod
    def steps(*pairs):
        return [{"turns_per_s": t, "master_cpu_pct": c} for t, c in pairs]

    def test_마스터_CPU_90퍼센트_이상이면_그_단계가_포화(self):
        s = self.steps((1000, 20), (2000, 50), (3500, 91), (3600, 99))
        self.assertEqual(tr.saturation_index(s), 2)

    def test_처리량_증가가_5퍼센트_미만이면_포화(self):
        s = self.steps((1000, 20), (2000, 50), (3000, 70), (3100, 80))
        self.assertEqual(tr.saturation_index(s), 3)

    def test_계속_늘면_포화_아님(self):
        s = self.steps((1000, 20), (2000, 40), (3900, 80))
        self.assertIsNone(tr.saturation_index(s))

    def test_처리량이_줄어도_포화(self):
        s = self.steps((1000, 20), (2000, 50), (1900, 60))
        self.assertEqual(tr.saturation_index(s), 2)


class PercentileTest(unittest.TestCase):
    def test_p95_는_가까운_순위(self):
        self.assertEqual(tr.percentile(list(range(1, 101)), 95), 95)
        self.assertEqual(tr.percentile([5], 95), 5)
        self.assertEqual(tr.percentile([], 95), 0.0)


if __name__ == "__main__":
    unittest.main()
