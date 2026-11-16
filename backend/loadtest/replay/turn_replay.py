"""채팅 한 턴의 Redis 명령 열(전·후)을 앱 없이 재생해 클러스터 포화를 잰다

표준 라이브러리만 사용 - RESP2 직접 구현, 클라이언트마다 노드별 TCP 연결 1개, 파이프라이닝 없음(명령마다 응답을 기다림)
사용: turn_replay.py step|pass|summarize ... (run.sh 가 컨테이너 안에서 부른다)
"""
import argparse
import hashlib
import json
import math
import multiprocessing as mp
import os
import platform
import random
import selectors
import socket
import sys
import time
import traceback
import uuid
from collections import namedtuple

TTL = 3600
TTL_B = b"3600"
HISTORY_WINDOW = 20
TURNS_PER_SESSION = 5
SESSIONS = 30000
STEPS = (8, 16, 32, 64, 128, 256)
EXTRA_STEP = 512
WARMUP_S = 3.0
MEASURE_S = 15.0
SATURATION_CPU_PCT = 90.0
PLATEAU_GROWTH = 1.05

# 방법값의 저장 스크립트 - 문장 4개를 바꾸면 시험이 깨진다
SAVE_SCRIPT = (
    "local n = redis.call('RPUSH', KEYS[1], ARGV[1])\n"
    "redis.call('EXPIRE', KEYS[1], ARGV[2])\n"
    "redis.call('EXPIRE', KEYS[2], ARGV[2])\n"
    "return n\n"
)
SAVE_SHA = hashlib.sha1(SAVE_SCRIPT.encode()).hexdigest()
SAVE_SHA_B = SAVE_SHA.encode()


def _load_claim_script() -> str:
    # 앱 파일을 그대로 쓴다 - 사본을 두면 앱이 바뀔 때 재생이 옛 스크립트로 돈다
    here = os.path.dirname(os.path.abspath(__file__))
    for path in (os.environ.get("CLAIM_LUA", ""),
                 os.path.join(here, "..", "..", "src", "main", "resources", "scripts", "chat-session-claim.lua")):
        if path and os.path.exists(path):
            with open(path, encoding="utf-8") as f:
                return f.read()
    return ""


CLAIM_SCRIPT = _load_claim_script()

# ---------------------------------------------------------------------------
# RESP2
# ---------------------------------------------------------------------------


class RespError(Exception):
    pass


def encode_command(args) -> bytes:
    parts = [b"*%d\r\n" % len(args)]
    for a in args:
        if isinstance(a, str):
            a = a.encode()
        elif isinstance(a, int):
            a = b"%d" % a
        parts.append(b"$%d\r\n%s\r\n" % (len(a), a))
    return b"".join(parts)


def decode_reply(buf, pos: int = 0):
    """buf[pos:] 에서 응답 하나를 읽어 (값, 다음 위치) 를 돌려준다 - 덜 왔으면 None"""
    end = buf.find(b"\r\n", pos)
    if end < 0:
        return None
    kind = buf[pos]
    body = buf[pos + 1:end]
    if kind == 43:  # +
        return bytes(body), end + 2
    if kind == 45:  # -
        return RespError(bytes(body).decode()), end + 2
    if kind == 58:  # :
        return int(body), end + 2
    if kind == 36:  # $
        n = int(body)
        if n < 0:
            return None, end + 2
        stop = end + 2 + n
        if len(buf) < stop + 2:
            return None
        return bytes(buf[end + 2:stop]), stop + 2
    if kind == 42:  # *
        n = int(body)
        if n < 0:
            return None, end + 2
        items = []
        p = end + 2
        for _ in range(n):
            got = decode_reply(buf, p)
            if got is None:
                return None
            items.append(got[0])
            p = got[1]
        return items, p
    raise ValueError("알 수 없는 RESP 접두 %r" % bytes([kind]))


# ---------------------------------------------------------------------------
# 클러스터 슬롯
# ---------------------------------------------------------------------------


def _crc16_table():
    table = []
    for i in range(256):
        crc = i << 8
        for _ in range(8):
            crc = ((crc << 1) ^ 0x1021) if crc & 0x8000 else (crc << 1)
        table.append(crc & 0xFFFF)
    return table


_CRC = _crc16_table()


def crc16(data: bytes) -> int:
    crc = 0
    for b in data:
        crc = ((crc << 8) & 0xFFFF) ^ _CRC[((crc >> 8) ^ b) & 0xFF]
    return crc


def key_slot(key) -> int:
    k = key.encode() if isinstance(key, str) else key
    s = k.find(b"{")
    if s >= 0:
        e = k.find(b"}", s + 1)
        if e > s + 1:  # 빈 태그 {} 는 키 전체를 해시
            k = k[s + 1:e]
    return crc16(k) % 16384


def message_key(sid: int) -> str:
    return "chat:{%d}:messages" % sid


def session_key(sid: int) -> str:
    return "chat:{%d}:session" % sid


# ---------------------------------------------------------------------------
# 메시지 본문 - 무작위 바이트가 아니라 JSON 모양 문장
# ---------------------------------------------------------------------------

_FILLER = ("Spring 에서 트랜잭션 경계는 서비스 계층에 두고 컨트롤러는 요청 검증과 응답 변환만 맡는다. "
           "Repository 는 영속 계층만 다룬다. ")


def make_message(role: str, size: int) -> bytes:
    head = '{"chatRole":"%s","message":"' % role
    tail = '","partial":false}'
    room = size - len(head.encode()) - len(tail.encode())
    # 한글은 UTF-8 3바이트 -> 바이트 길이로 자른 뒤 깨진 끝 글자는 ASCII 로 채운다
    text = (_FILLER * (room // 20 + 2)).encode()[:room]
    text = text.decode("utf-8", errors="ignore")
    text += "." * (room - len(text.encode()))
    out = (head + text + tail).encode()
    assert len(out) == size, (len(out), size)
    return out


QUESTION = make_message("USER", 150)
ANSWER = make_message("ASSISTANT", 2400)

# ---------------------------------------------------------------------------
# 턴 명령 열 - 생성기가 명령을 내놓고 응답을 받아 다음 명령을 정한다
# ---------------------------------------------------------------------------

Cmd = namedtuple("Cmd", "role args")


def turn_before(sid: int):
    """오늘 코드의 한 턴 - 8명령(master 7 · replica 1)"""
    m = message_key(sid).encode()
    s = session_key(sid).encode()
    info = {"owner_recheck": False, "fallback": False}
    owner = yield Cmd("replica", [b"HGET", s, b"userId"])
    if owner is None:
        # assertSessionOwner 와 같다 - replica 가 비면 master 로 재확인
        info["owner_recheck"] = True
        yield Cmd("master", [b"HGET", s, b"userId"])
    yield Cmd("master", [b"RPUSH", m, QUESTION])
    yield Cmd("master", [b"EXPIRE", m, TTL_B])
    yield Cmd("master", [b"EXPIRE", s, TTL_B])
    yield Cmd("master", [b"LRANGE", m, b"%d" % -HISTORY_WINDOW, b"-1"])
    yield Cmd("master", [b"RPUSH", m, ANSWER])
    yield Cmd("master", [b"EXPIRE", m, TTL_B])
    yield Cmd("master", [b"EXPIRE", s, TTL_B])
    return info


def turn_after(sid: int):
    """t1 코드의 한 턴 - 4명령(master 2 · replica 2), 첫 턴은 이력 읽기가 없어 3명령"""
    m = message_key(sid).encode()
    s = session_key(sid).encode()
    info = {"owner_recheck": False, "fallback": False}
    owner = yield Cmd("replica", [b"HGET", s, b"userId"])
    if owner is None:
        info["owner_recheck"] = True
        yield Cmd("master", [b"HGET", s, b"userId"])
    length = yield Cmd("master", [b"EVALSHA", SAVE_SHA_B, b"2", m, s, QUESTION, TTL_B])
    if length > 1:
        start = max(0, length - HISTORY_WINDOW)
        stop = length - 2
        read = [b"LRANGE", m, b"%d" % start, b"%d" % stop]
        items = yield Cmd("replica", read)
        if len(items) < length - 1 - start:
            # 복제 지연 - 같은 범위를 master 에서 한 번 더 읽고 센다
            info["fallback"] = True
            yield Cmd("master", read)
    yield Cmd("master", [b"EVALSHA", SAVE_SHA_B, b"2", m, s, ANSWER, TTL_B])
    return info


def session_reset(sid: int, owner: str, now_ms: int):
    """세션 종료와 새 소유자 선점 - 세션 부담으로 따로 센다"""
    if not CLAIM_SCRIPT:
        raise RuntimeError("chat-session-claim.lua 를 찾지 못함 (CLAIM_LUA)")
    s = session_key(sid).encode()
    yield Cmd("master", [b"UNLINK", message_key(sid).encode()])
    yield Cmd("master", [b"UNLINK", s])
    # EVAL 로 보낸다 - commandstats 에서 저장의 EVALSHA 와 갈라 세션 부담만 따로 읽으려는 것
    yield Cmd("master", [b"EVAL", CLAIM_SCRIPT.encode(), b"1", s, owner.encode(), TTL_B, b"%d" % now_ms])


SEQUENCES = {"before": turn_before, "after": turn_after}

# ---------------------------------------------------------------------------
# 판정 보조
# ---------------------------------------------------------------------------


def percentile(values, p: float) -> float:
    if not values:
        return 0.0
    ordered = sorted(values)
    return ordered[max(0, math.ceil(p / 100.0 * len(ordered)) - 1)]


def saturation_index(steps):
    """가장 바쁜 master CPU 90% 이상이거나 처리량 증가가 직전 단계 대비 5% 미만인 첫 단계, 없으면 None"""
    for i, st in enumerate(steps):
        if st["master_cpu_pct"] >= SATURATION_CPU_PCT:
            return i
        if i > 0 and st["turns_per_s"] < steps[i - 1]["turns_per_s"] * PLATEAU_GROWTH:
            return i
    return None


# ---------------------------------------------------------------------------
# 연결과 토폴로지
# ---------------------------------------------------------------------------


class Conn:
    __slots__ = ("sock", "buf", "host", "port")

    def __init__(self, host: str, port: int, readonly: bool = False):
        self.host, self.port = host, port
        self.sock = socket.create_connection((host, port), timeout=30)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        self.sock.settimeout(None)
        self.buf = bytearray()
        if readonly:
            self.call([b"READONLY"])

    def read_reply(self):
        while True:
            got = decode_reply(self.buf)
            if got is not None:
                value, used = got
                del self.buf[:used]
                return value
            data = self.sock.recv(1 << 18)
            if not data:
                raise ConnectionError("%s:%d 연결 끊김" % (self.host, self.port))
            self.buf += data

    def call(self, args):
        self.sock.sendall(encode_command(args))
        value = self.read_reply()
        if isinstance(value, RespError):
            raise RuntimeError("%s:%d %s -> %s" % (self.host, self.port, args[0], value))
        return value

    def pipeline(self, commands):
        """준비 단계 전용 - 측정 경로는 쓰지 않는다"""
        self.sock.sendall(b"".join(encode_command(c) for c in commands))
        out = []
        for _ in commands:
            value = self.read_reply()
            if isinstance(value, RespError):
                raise RuntimeError("%s:%d 파이프라인 오류 %s" % (self.host, self.port, value))
            out.append(value)
        return out


class Topology:
    """CLUSTER SLOTS 로 읽은 슬롯 -> master·replica 노드 대응"""

    def __init__(self, seed: str):
        host, port = seed.rsplit(":", 1)
        conn = Conn(host, int(port))
        rows = conn.call([b"CLUSTER", b"SLOTS"])
        conn.sock.close()
        self.nodes = []
        self.master_idx = []
        self.replica_idx = []
        self.slot_master = [-1] * 16384
        self.slot_replica = [-1] * 16384

        def node(entry) -> int:
            h = entry[0].decode() or host
            key = (h, int(entry[1]))
            if key not in self.nodes:
                self.nodes.append(key)
            return self.nodes.index(key)

        for row in rows:
            m = node(row[2])
            if m not in self.master_idx:
                self.master_idx.append(m)
            if len(row) < 4:
                raise RuntimeError("replica 없는 슬롯 범위 %d-%d" % (row[0], row[1]))
            r = node(row[3])
            if r not in self.replica_idx:
                self.replica_idx.append(r)
            for slot in range(row[0], row[1] + 1):
                self.slot_master[slot] = m
                self.slot_replica[slot] = r
        if -1 in self.slot_master:
            raise RuntimeError("슬롯 16384 개가 다 배정되지 않음")


def parse_info(text: str):
    cpu = 0.0
    mem = 0
    cmd = {}
    for line in text.splitlines():
        if line.startswith("used_cpu_sys:") or line.startswith("used_cpu_user:"):
            cpu += float(line.split(":", 1)[1])
        elif line.startswith("used_memory:"):
            mem = int(line.split(":", 1)[1])
        elif line.startswith("cmdstat_"):
            name, rest = line.split(":", 1)
            fields = dict(kv.split("=") for kv in rest.split(","))
            cmd[name[8:]] = (int(fields["calls"]), float(fields["usec"]))
    return {"cpu": cpu, "mem": mem, "cmd": cmd}


def snapshot(conn: Conn):
    snap = parse_info(conn.call([b"INFO", b"cpu", b"commandstats", b"memory"]).decode())
    snap["t"] = time.monotonic()
    return snap


def node_delta(a, b):
    dt = b["t"] - a["t"]
    cmd = {}
    for name, (calls, usec) in b["cmd"].items():
        c0, u0 = a["cmd"].get(name, (0, 0.0))
        if calls - c0:
            cmd[name] = {"calls": calls - c0, "usec": usec - u0}
    return {"cpu_s": b["cpu"] - a["cpu"], "wall_s": dt, "cpu_pct": (b["cpu"] - a["cpu"]) / dt * 100.0,
            "cmd": cmd, "mem": b["mem"]}


# ---------------------------------------------------------------------------
# 준비 - 스크립트 적재, 세션 전부 비우고 소유자 선점
# ---------------------------------------------------------------------------


def prepare_pass(topo: Topology, sessions: int):
    """모든 master 에 저장 스크립트를 적재하고 세션 30,000개를 빈 상태 + 새 소유자로 맞춘다(파이프라인 사용, 측정 밖)"""
    conns = {i: Conn(*topo.nodes[i]) for i in topo.master_idx}
    for conn in conns.values():
        sha = conn.call([b"SCRIPT", b"LOAD", SAVE_SCRIPT.encode()]).decode()
        if sha != SAVE_SHA:
            raise RuntimeError("스크립트 SHA 불일치 %s != %s" % (sha, SAVE_SHA))
    per_node = {i: [] for i in conns}
    now_ms = int(time.time() * 1000)
    for sid in range(1, sessions + 1):
        node = topo.slot_master[key_slot(message_key(sid))]
        per_node[node].extend(c.args for c in session_reset(sid, "u-%d-%s" % (sid, uuid.uuid4().hex[:8]), now_ms))
    for i, commands in per_node.items():
        for at in range(0, len(commands), 900):
            conns[i].pipeline(commands[at:at + 900])
    for conn in conns.values():
        conn.sock.close()


# ---------------------------------------------------------------------------
# 작업 프로세스 - 가상 클라이언트 여럿을 selectors 한 루프에서 돌린다
# ---------------------------------------------------------------------------


class VClient:
    __slots__ = ("gi", "conns", "sid", "conn", "gen", "master_n", "replica_n", "hist_reads")

    def __init__(self, gi: int):
        self.gi = gi
        self.conns = []
        self.sid = 0
        self.conn = None
        self.gen = None
        self.master_n = 0
        self.replica_n = 0
        self.hist_reads = 0


def client_life(c: VClient, seq: str, clients: int, sessions: int, ages, seed: int, t1: float, t2: float, acc: dict):
    """한 클라이언트의 삶 - 자기 세션에서 무작위로 골라 턴을 돌리고, 5턴 찬 세션은 정리·재선점"""
    rng = random.Random(seed)
    own = len(range(c.gi + 1, sessions + 1, clients))
    turn = SEQUENCES[seq]
    lat = acc["lat"]
    while time.monotonic() < t2:
        sid = c.gi + 1 + clients * rng.randrange(own)
        c.sid = sid
        m0, r0, h0 = c.master_n, c.replica_n, c.hist_reads
        started = time.monotonic()
        info = yield from turn(sid)
        ended = time.monotonic()
        age = ages[sid - 1]
        if t1 <= ended < t2:
            acc["turns"] += 1
            acc["master_trips"] += c.master_n - m0
            acc["replica_trips"] += c.replica_n - r0
            acc["hist_reads"] += c.hist_reads - h0
            acc["owner_recheck"] += info["owner_recheck"]
            acc["fallback"] += info["fallback"]
            acc["k_hist"][age] += 1
            lat.append((ended - started) * 1000.0)
        if age + 1 >= TURNS_PER_SESSION:
            ages[sid - 1] = 0
            yield from session_reset(sid, "u-%d-%s" % (sid, uuid.uuid4().hex[:8]), int(time.time() * 1000))
            if t1 <= time.monotonic() < t2:
                acc["resets"] += 1
        else:
            ages[sid - 1] = age + 1


def worker_main(wi: int, client_ids, args, ages, barrier1, barrier2, t0_value, queue):
    try:
        topo = args["topo"]
        slot_of = [0] + [key_slot(message_key(s)) for s in range(1, args["sessions"] + 1)]
        master_of = topo.slot_master
        replica_of = topo.slot_replica
        clients = []
        for gi in client_ids:
            c = VClient(gi)
            for n, (h, p) in enumerate(topo.nodes):
                c.conns.append(Conn(h, p, readonly=n in topo.replica_idx))
            clients.append(c)
        barrier1.wait(timeout=120)
        barrier2.wait(timeout=120)
        t0 = t0_value.value
        t1 = t0 + args["warmup"]
        t2 = t1 + args["measure"]
        acc = {"turns": 0, "master_trips": 0, "replica_trips": 0, "hist_reads": 0, "owner_recheck": 0,
               "fallback": 0, "resets": 0, "k_hist": [0] * TURNS_PER_SESSION, "lat": []}
        sel = selectors.DefaultSelector()
        for c in clients:
            for conn in c.conns:
                sel.register(conn.sock, selectors.EVENT_READ, (c, conn))

        def send(c: VClient, cmd: Cmd):
            node = master_of[slot_of[c.sid]] if cmd.role == "master" else replica_of[slot_of[c.sid]]
            if cmd.role == "master":
                c.master_n += 1
            else:
                c.replica_n += 1
                if cmd.args[0] == b"LRANGE":
                    c.hist_reads += 1
            conn = c.conns[node]
            c.conn = conn
            conn.sock.sendall(encode_command(cmd.args))

        while time.monotonic() < t0:
            time.sleep(0.001)
        active = 0
        for c in clients:
            c.gen = client_life(c, args["seq"], args["clients"], args["sessions"], ages,
                                args["seed"] * 100003 + c.gi, t1, t2, acc)
            try:
                send(c, next(c.gen))
                active += 1
            except StopIteration:
                pass
        cpu0 = cpu1 = None
        while active:
            now = time.monotonic()
            if cpu0 is None and now >= t1:
                cpu0 = time.process_time()
            if cpu1 is None and now >= t2:
                cpu1 = time.process_time()
            for key, _ in sel.select(1.0):
                c, conn = key.data
                data = conn.sock.recv(1 << 18)
                if not data:
                    raise ConnectionError("%s:%d 연결 끊김" % (conn.host, conn.port))
                conn.buf += data
                got = decode_reply(conn.buf)
                if got is None:
                    continue
                value, used = got
                del conn.buf[:used]
                if isinstance(value, RespError):
                    raise RuntimeError("%s:%d 응답 오류 %s (세션 %d)" % (conn.host, conn.port, value, c.sid))
                try:
                    send(c, c.gen.send(value))
                except StopIteration:
                    active -= 1
        if cpu1 is None:
            cpu1 = time.process_time()
        if cpu0 is None:
            cpu0 = cpu1
        acc["cpu_s"] = cpu1 - cpu0
        queue.put(acc)
    except BaseException:
        queue.put({"error": "worker %d: %s" % (wi, traceback.format_exc())})
        raise


# ---------------------------------------------------------------------------
# 단계 실행
# ---------------------------------------------------------------------------


def run_step(topo: Topology, seq: str, clients: int, ages, procs_max: int, warmup: float, measure: float,
             sessions: int, seed: int):
    procs = min(clients, procs_max)
    ctx = mp.get_context("fork")
    barrier1 = ctx.Barrier(procs + 1)
    barrier2 = ctx.Barrier(procs + 1)
    t0_value = ctx.Value("d", 0.0)
    queue = ctx.Queue()
    args = {"topo": topo, "seq": seq, "clients": clients, "sessions": sessions, "warmup": warmup,
            "measure": measure, "seed": seed}
    workers = [ctx.Process(target=worker_main,
                           args=(p, list(range(p, clients, procs)), args, ages, barrier1, barrier2, t0_value, queue))
               for p in range(procs)]
    for w in workers:
        w.start()
    conns = [Conn(h, p) for h, p in topo.nodes]
    try:
        try:
            barrier1.wait(timeout=120)
        except mp.BrokenBarrierError:
            errors = []
            while not queue.empty():
                errors.append(queue.get())
            raise RuntimeError("작업 프로세스 연결 실패: %s" % errors)
        t0_value.value = time.monotonic() + 0.3
        barrier2.wait(timeout=60)
        t1 = t0_value.value + warmup
        t2 = t1 + measure
        while time.monotonic() < t1:
            time.sleep(0.0005)
        snaps1 = [snapshot(c) for c in conns]
        while time.monotonic() < t2:
            time.sleep(0.0005)
        snaps2 = [snapshot(c) for c in conns]
        results = [queue.get(timeout=measure + 60) for _ in workers]
    finally:
        for c in conns:
            c.sock.close()
        for w in workers:
            w.join(timeout=30)
            if w.is_alive():
                w.terminate()
    errors = [r["error"] for r in results if "error" in r]
    if errors:
        raise RuntimeError("작업 프로세스 오류: %s" % errors[0])

    turns = sum(r["turns"] for r in results)
    lat = [x for r in results for x in r["lat"]]
    deltas = [node_delta(a, b) for a, b in zip(snaps1, snaps2)]
    masters = [deltas[i] for i in topo.master_idx]
    replicas = [deltas[i] for i in topo.replica_idx]

    def role_cmds(group):
        out = {}
        for d in group:
            for name, v in d["cmd"].items():
                o = out.setdefault(name, {"calls": 0, "usec": 0.0})
                o["calls"] += v["calls"]
                o["usec"] += v["usec"]
        return out

    mcmd, rcmd = role_cmds(masters), role_cmds(replicas)
    per = (lambda x: x / turns) if turns else (lambda x: 0.0)
    resets = sum(r["resets"] for r in results)
    overhead_usec = sum(mcmd.get(n, {"usec": 0.0})["usec"] for n in ("unlink", "eval"))
    hist = sum(r["hist_reads"] for r in results)
    fallback = sum(r["fallback"] for r in results)
    gen_cpu = sum(r["cpu_s"] for r in results) / measure * 100.0
    return {
        "clients": clients,
        "procs": procs,
        "turns": turns,
        "turns_per_s": turns / measure,
        "master_cpu_pct": max(d["cpu_pct"] for d in masters),
        "replica_cpu_pct": max(d["cpu_pct"] for d in replicas),
        "master_cpu_pct_by_node": [round(d["cpu_pct"], 1) for d in masters],
        "replica_cpu_pct_by_node": [round(d["cpu_pct"], 1) for d in replicas],
        "master_round_trips_per_turn": per(sum(r["master_trips"] for r in results)),
        "replica_round_trips_per_turn": per(sum(r["replica_trips"] for r in results)),
        "turn_p50_ms": percentile(lat, 50),
        "turn_p95_ms": percentile(lat, 95),
        "turn_p99_ms": percentile(lat, 99),
        "generator_cpu_pct": gen_cpu,
        "generator_cpu_pct_per_proc": gen_cpu / procs,
        "master_cpu_us_per_turn": per(sum(d["cpu_s"] for d in masters) * 1e6),
        "replica_cpu_us_per_turn": per(sum(d["cpu_s"] for d in replicas) * 1e6),
        "owner_recheck_rate": per(sum(r["owner_recheck"] for r in results)),
        "history_reads": hist,
        "fallbacks": fallback,
        "fallback_rate": (fallback / hist) if hist else 0.0,
        "session_resets": resets,
        "session_overhead_master_per_turn": per(resets * 3),
        "session_overhead_cmdstat_usec_per_turn": per(overhead_usec),
        "k_hist": [sum(r["k_hist"][k] for r in results) for k in range(TURNS_PER_SESSION)],
        "master_commandstats": mcmd,
        "replica_commandstats": rcmd,
        "used_memory_master_mb": sum(d["mem"] for d in masters) / 1e6,
        "measure_s": measure,
        "warmup_s": warmup,
    }


def finish_pass(seq: str, steps):
    idx = saturation_index(steps)
    before = steps[max(0, idx - 1)] if idx is not None else steps[-1]
    last = steps[idx] if idx is not None else steps[-1]
    cores = os.cpu_count() or 1
    busy = last["generator_cpu_pct"] / (100.0 * last["procs"])
    if last["master_cpu_pct"] >= SATURATION_CPU_PCT:
        limited = "master"
    elif idx is not None and busy >= 0.85:
        limited = "generator"
    else:
        limited = "plateau-unexplained" if idx is not None else "none"
    return {
        "sequence": seq,
        "steps": steps,
        "saturation_step": idx,
        "saturated": idx is not None,
        "limited_by": limited,
        "generator_busy_ratio_at_saturation": busy,
        "generator_cores": cores,
        "max_turns_per_s": max(s["turns_per_s"] for s in steps),
        "master_cpu_us_per_turn": before["master_cpu_us_per_turn"],
        "replica_cpu_us_per_turn": before["replica_cpu_us_per_turn"],
    }


def environment(seed: str):
    return {"python": platform.python_version(), "cpu_count": os.cpu_count(), "seed_node": seed,
            "kernel": platform.release(), "started_at": time.strftime("%Y-%m-%dT%H:%M:%S%z")}


def run_pass(a):
    topo = Topology(a.seed_node)
    prepare_pass(topo, a.sessions)
    time.sleep(2.0)  # 준비 때 쓴 명령이 replica 에 다 반영되도록
    ages = mp.get_context("fork").RawArray("B", a.sessions)
    steps = []
    plan = list(a.steps)
    i = 0
    while i < len(plan):
        n = plan[i]
        st = run_step(topo, a.seq, n, ages, a.procs_max, a.warmup, a.measure, a.sessions, a.seed + i)
        steps.append(st)
        print("step %-4d turns/s %9.0f  master %5.1f%%  replica %5.1f%%  gen %6.0f%%  p95 %7.2f ms  fb %.4f"
              % (n, st["turns_per_s"], st["master_cpu_pct"], st["replica_cpu_pct"], st["generator_cpu_pct"],
                 st["turn_p95_ms"], st["fallback_rate"]), flush=True)
        i += 1
        if i == len(plan) and n == STEPS[-1] and a.auto_extra and saturation_index(steps) is None:
            plan.append(EXTRA_STEP)
    out = finish_pass(a.seq, steps)
    out["env"] = environment(a.seed_node)
    out["sessions"] = a.sessions
    out["procs_max"] = a.procs_max
    write_json(a.out, out)
    print("pass 끝 saturated=%s limited_by=%s max_turns_per_s=%.0f" % (out["saturated"], out["limited_by"],
                                                                        out["max_turns_per_s"]), flush=True)


def run_one_step(a):
    topo = Topology(a.seed_node)
    prepare_pass(topo, a.sessions)
    time.sleep(1.0)
    ages = mp.get_context("fork").RawArray("B", a.sessions)
    st = run_step(topo, a.seq, a.clients, ages, a.procs_max, a.warmup, a.measure, a.sessions, a.seed)
    write_json(a.out, {"sequence": a.seq, "steps": [st], "env": environment(a.seed_node)})
    print(json.dumps({k: v for k, v in st.items() if "commandstats" not in k}, indent=1, ensure_ascii=False))


def write_json(path: str, obj):
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=1, ensure_ascii=False)


# ---------------------------------------------------------------------------
# 요약
# ---------------------------------------------------------------------------

STEP_KEYS = ("clients", "turns_per_s", "master_cpu_pct", "replica_cpu_pct", "master_round_trips_per_turn",
             "replica_round_trips_per_turn", "turn_p95_ms", "generator_cpu_pct")


def _mean(xs):
    return sum(xs) / len(xs)


def summarize_sequence(passes):
    steps = []
    for n, p in enumerate(passes, start=1):
        for st in p["steps"]:
            row = {k: st[k] for k in STEP_KEYS}
            row["pass"] = n
            for extra in ("master_cpu_us_per_turn", "replica_cpu_us_per_turn", "fallback_rate", "owner_recheck_rate",
                          "turn_p50_ms", "turn_p99_ms", "procs", "session_overhead_master_per_turn"):
                row[extra] = st[extra]
            steps.append(row)
    hist = sum(st["history_reads"] for p in passes for st in p["steps"])
    fb = sum(st["fallbacks"] for p in passes for st in p["steps"])
    maxes = [p["max_turns_per_s"] for p in passes]
    cpu = [p["master_cpu_us_per_turn"] for p in passes]
    rcpu = [p["replica_cpu_us_per_turn"] for p in passes]
    last = [p["steps"][-1] for p in passes]
    return {
        "steps": steps,
        "max_turns_per_s": _mean(maxes),
        "max_turns_per_s_by_pass": maxes,
        "saturated": all(p["saturated"] for p in passes),
        "limited_by_by_pass": [p["limited_by"] for p in passes],
        "master_cpu_us_per_turn": _mean(cpu),
        "master_cpu_us_per_turn_by_pass": cpu,
        "replica_cpu_us_per_turn": _mean(rcpu),
        "replica_cpu_us_per_turn_by_pass": rcpu,
        "replica_cpu_share": _mean(rcpu) / (_mean(rcpu) + _mean(cpu)),
        "busiest_master_cpu_pct_at_last_step_by_pass": [s["master_cpu_pct"] for s in last],
        "fallback_rate": (fb / hist) if hist else 0.0,
        "fallbacks": fb,
        "history_reads": hist,
        "passes": len(passes),
    }


def run_summarize(a):
    base = [json.load(open(p, encoding="utf-8")) for p in a.base]
    new = [json.load(open(p, encoding="utf-8")) for p in a.new]
    out = {"base": summarize_sequence(base), "new": summarize_sequence(new)}
    b, n = out["base"], out["new"]
    out["ratio_new_over_base"] = {
        "max_turns_per_s": n["max_turns_per_s"] / b["max_turns_per_s"],
        "master_cpu_us_per_turn": n["master_cpu_us_per_turn"] / b["master_cpu_us_per_turn"],
    }
    out["env"] = base[0].get("env", {})
    write_json(a.out, out)
    print(json.dumps(out["ratio_new_over_base"]))


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__)
    sub = ap.add_subparsers(dest="cmd", required=True)

    def common(p):
        p.add_argument("--seq", choices=sorted(SEQUENCES), required=True)
        p.add_argument("--seed-node", default=os.environ.get("SEED_NODE", "rp-node-1:6379"))
        p.add_argument("--sessions", type=int, default=SESSIONS)
        p.add_argument("--procs-max", type=int, default=int(os.environ.get("PROCS_MAX", "8")))
        p.add_argument("--warmup", type=float, default=WARMUP_S)
        p.add_argument("--measure", type=float, default=MEASURE_S)
        p.add_argument("--seed", type=int, default=1)
        p.add_argument("--out", required=True)

    p = sub.add_parser("pass")
    common(p)
    p.add_argument("--steps", type=int, nargs="+", default=list(STEPS))
    p.add_argument("--no-auto-extra", dest="auto_extra", action="store_false")
    p = sub.add_parser("step")
    common(p)
    p.add_argument("--clients", type=int, required=True)
    p = sub.add_parser("summarize")
    p.add_argument("--base", nargs="+", required=True)
    p.add_argument("--new", nargs="+", required=True)
    p.add_argument("--out", required=True)
    a = ap.parse_args(argv)
    {"pass": run_pass, "step": run_one_step, "summarize": run_summarize}[a.cmd](a)


if __name__ == "__main__":
    main()
