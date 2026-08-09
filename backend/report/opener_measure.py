#!/usr/bin/env python3
"""오프너 분석 경로 벡터 검색 실측 - rag.vector.search{path=opener}

Task 12 가 일반 채팅 경로를 잰 것과 같은 방식으로 오프너 경로를 잰다
문서에서 내린 "80ms 이내" 를 대체할 값을 얻는 것이 목적
"""
import json
import os
import subprocess
import sys
import time

BASE = "http://localhost:8080/api"
SCRATCH = os.path.dirname(os.path.abspath(__file__))
COOKIES = os.path.join(SCRATCH, "cookies.txt")
QUESTION_ID = 900001
QUESTION_RESULT_ID = 900001
N_WARMUP = 2
N_SAMPLE = 12          # 워밍업 2 + 본표본 10 (오프너는 LLM 응답이 길어 회당 비용이 큼)


def curl_json(path):
    out = subprocess.run(
        ["curl", "-s", "-b", COOKIES, "-w", "\n%{http_code}", BASE + path],
        capture_output=True, text=True,
    ).stdout
    body, _, code = out.rpartition("\n")
    return json.loads(body) if code.strip() == "200" else None


def read_timer(path_tag):
    d = curl_json("/actuator/metrics/rag.vector.search?tag=path:%s" % path_tag)
    if d is None:
        return 0, 0.0
    m = {s["statistic"]: s["value"] for s in d["measurements"]}
    return int(m["COUNT"]), float(m["TOTAL_TIME"])


def run_once(session_id):
    sse_path = os.path.join(SCRATCH, "sse_op_%d.log" % session_id)
    f = open(sse_path, "w")
    sse = subprocess.Popen(
        ["curl", "-sN", "-b", COOKIES, "%s/chat/connect?sessionId=%d" % (BASE, session_id)],
        stdout=f, stderr=subprocess.STDOUT,
    )
    for _ in range(100):
        time.sleep(0.1)
        if os.path.getsize(sse_path) > 0 and "connect" in open(sse_path).read().lower():
            break
    else:
        sse.terminate(); f.close(); return None, "no-connect"

    before_c, before_t = read_timer("opener")
    payload = json.dumps({"sessionId": session_id,
                          "questionResultId": QUESTION_RESULT_ID,
                          "questionId": QUESTION_ID})
    code = subprocess.run(
        ["curl", "-s", "-b", COOKIES, "-o", "/dev/null", "-w", "%{http_code}",
         "-X", "POST", BASE + "/chat/analysis",
         "-H", "Content-Type: application/json", "-d", payload],
        capture_output=True, text=True,
    ).stdout.strip()
    if code != "200":
        sse.terminate(); f.close(); return None, "post-" + code

    val = None
    deadline = time.time() + 60
    while time.time() < deadline:
        time.sleep(0.15)
        c, t = read_timer("opener")
        if c > before_c:
            val = (t - before_t) / (c - before_c)
            break

    time.sleep(0.6)
    sse.terminate(); f.close()
    return (val, "ok") if val is not None else (None, "timeout")


def main():
    results = []
    base = int(time.time()) % 100000 * 100
    for i in range(N_SAMPLE):
        sid = base + i
        v, st = run_once(sid)
        tag = "warmup" if i < N_WARMUP else "sample"
        results.append({"i": i, "ttl": v, "status": st, "tag": tag})
        print("[%02d] %-6s sid=%d %s %s" % (i, tag, sid, st,
              ("%.3fs" % v) if v else "-"), flush=True)

    json.dump(results, open(os.path.join(SCRATCH, "opener_raw.json"), "w"), indent=1)
    ok = sorted(r["ttl"] for r in results if r["tag"] == "sample" and r["ttl"])
    if not ok:
        print("본표본 0건"); return 1
    n = len(ok)
    pct = lambda p: ok[min(n - 1, max(0, round(p * (n - 1))))]
    print("\n=== rag.vector.search{path=opener} 본표본 ===")
    print("n=%d  min=%.0fms  중앙값=%.0fms  평균=%.0fms  p95=%.0fms  max=%.0fms"
          % (n, ok[0] * 1000, pct(.5) * 1000, sum(ok) / n * 1000, pct(.95) * 1000, ok[-1] * 1000))
    return 0


if __name__ == "__main__":
    sys.exit(main())
