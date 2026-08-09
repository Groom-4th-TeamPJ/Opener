#!/usr/bin/env python3
"""chat.sse.ttft 실측 - Timer 누적값 델타로 개별 요청 TTFT 를 뽑는다"""
import json
import os
import subprocess
import sys
import time
import urllib.request
import http.cookiejar

BASE = "http://localhost:8080/api"
SCRATCH = os.path.dirname(os.path.abspath(__file__))
COOKIES = os.path.join(SCRATCH, "cookies.txt")
N_WARMUP = 2
N_SAMPLE = 22          # 워밍업 2 + 본표본 20
QUESTIONS = [
    "수능 국어 비문학 지문을 빨리 읽는 방법을 알려줘",
    "미적분에서 극한의 개념을 쉽게 설명해줘",
    "영어 문법에서 관계대명사가 뭔지 알려줘",
    "한국사에서 조선 후기 실학의 특징을 알려줘",
    "물리에서 관성의 법칙을 예시로 설명해줘",
]


def curl_json(path):
    """actuator 조회 - 쿠키 필요"""
    out = subprocess.run(
        ["curl", "-s", "-b", COOKIES, "-w", "\n%{http_code}", BASE + path],
        capture_output=True, text=True,
    ).stdout
    body, _, code = out.rpartition("\n")
    if code.strip() != "200":
        return None
    return json.loads(body)


def read_timer():
    """(count, total_seconds) 반환. 미터 미등록이면 (0, 0.0)"""
    d = curl_json("/actuator/metrics/chat.sse.ttft")
    if d is None:
        return 0, 0.0
    m = {s["statistic"]: s["value"] for s in d["measurements"]}
    return int(m["COUNT"]), float(m["TOTAL_TIME"])


def post_message(session_id, question):
    payload = json.dumps({"sessionId": session_id, "questionId": 1, "message": question})
    r = subprocess.run(
        ["curl", "-s", "-b", COOKIES, "-o", "/dev/null", "-w", "%{http_code}",
         "-X", "POST", BASE + "/chat/message",
         "-H", "Content-Type: application/json", "-d", payload],
        capture_output=True, text=True,
    )
    return r.stdout.strip()


def run_once(i, session_id, question):
    sse_path = os.path.join(SCRATCH, f"sse_{session_id}.log")
    sse_file = open(sse_path, "w")
    sse = subprocess.Popen(
        ["curl", "-sN", "-b", COOKIES, f"{BASE}/chat/connect?sessionId={session_id}"],
        stdout=sse_file, stderr=subprocess.STDOUT,
    )

    # connected 이벤트 대기 - Sink 등록 전에 POST 하면 SESSION_EXPIRED
    connected = False
    for _ in range(100):
        time.sleep(0.1)
        if os.path.getsize(sse_path) > 0:
            with open(sse_path) as f:
                if "connect" in f.read().lower():
                    connected = True
                    break
    if not connected:
        sse.terminate(); sse_file.close()
        return None, "no-connect"

    before_count, before_total = read_timer()
    code = post_message(session_id, question)
    if code != "200":
        sse.terminate(); sse_file.close()
        return None, f"post-{code}"

    # 카운트가 오를 때까지 폴링 - 오르는 순간의 델타가 이 요청의 TTFT
    ttft = None
    deadline = time.time() + 60
    while time.time() < deadline:
        time.sleep(0.15)
        c, t = read_timer()
        if c > before_count:
            ttft = (t - before_total) / (c - before_count)
            break

    time.sleep(1.0)   # 스트림 마무리 여유
    sse.terminate()
    sse_file.close()
    if ttft is None:
        return None, "timeout"
    return ttft, "ok"


def main():
    results = []
    base_session = int(time.time()) % 100000 * 100
    for i in range(N_SAMPLE):
        sid = base_session + i
        q = QUESTIONS[i % len(QUESTIONS)]
        ttft, status = run_once(i, sid, q)
        tag = "warmup" if i < N_WARMUP else "sample"
        results.append({"i": i, "sessionId": sid, "ttft_s": ttft, "status": status, "tag": tag})
        print(f"[{i:02d}] {tag:6s} sid={sid} status={status} "
              f"ttft={'%.3f' % ttft if ttft else '-'}s", flush=True)

    with open(os.path.join(SCRATCH, "ttft_raw.json"), "w") as f:
        json.dump(results, f, ensure_ascii=False, indent=2)

    ok = [r["ttft_s"] for r in results if r["tag"] == "sample" and r["ttft_s"] is not None]
    if not ok:
        print("본표본 0건 - 실패 원인 확인 필요")
        return 1
    ok_sorted = sorted(ok)
    n = len(ok_sorted)

    def pct(p):
        idx = min(n - 1, max(0, int(round(p * (n - 1)))))
        return ok_sorted[idx]

    print("\n=== 본표본 집계 (워밍업 %d회 제외) ===" % N_WARMUP)
    print(f"n       = {n}")
    print(f"min     = {ok_sorted[0]*1000:.0f} ms")
    print(f"median  = {pct(0.5)*1000:.0f} ms")
    print(f"mean    = {sum(ok)/n*1000:.0f} ms")
    print(f"p95     = {pct(0.95)*1000:.0f} ms")
    print(f"max     = {ok_sorted[-1]*1000:.0f} ms")
    return 0


if __name__ == "__main__":
    sys.exit(main())
