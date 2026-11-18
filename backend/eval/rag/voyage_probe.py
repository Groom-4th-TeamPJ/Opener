#!/usr/bin/env python3
"""Voyage 무료(카드 없음) 한도 탐색 (r129 step 2) - 실제 말뭉치 청크를 1·2·4·8… 개씩 늘려 보내다 첫 429 또는 토큰 상한에서 멈춘다
사용: voyage_probe.py [--model voyage-4] [--cap 50000]   (cpu-lock 안에서 실행, 키는 backend/.env 에서만 읽고 출력하지 않는다)
결과: R/measure/rag-eval/voyage-limit.json - 요청별 기록·분당 집계·429 응답 본문과 한도 관련 헤더·색인 1회 소요 시간 추정
"""
import argparse
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import evalkit  # noqa: E402
import ragio  # noqa: E402

FULL_INDEX_TOKENS = 1_200_000  # t3 보고서 ① 의 추정 (한글 1토큰/글자), 이 실행에서 실측 토큰/글자 비율로 다시 계산해 함께 적는다


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", default="voyage-4")
    ap.add_argument("--cap", type=int, default=50_000)
    ap.add_argument("--out", default=os.path.join(ragio.OUT_DIR, "voyage-limit.json"))
    a = ap.parse_args()
    key = ragio.load_key("VOYAGE_API_KEY")
    rows = ragio.json_rows(ragio.SRC_DB, "SELECT json_build_object('id',id,'chars',length(content),'content',content) FROM vector_store "
                                        "WHERE metadata->>'source'='MATH-ko-test.jsonl' ORDER BY md5(id::text) LIMIT 600")
    total_chars = int(ragio.psql(ragio.SRC_DB, "SELECT sum(length(content)) FROM vector_store").strip())
    ledger, sent, used, size, stopped = [], 0, 0, 1, "cap"
    t_start = time.time()
    while used < a.cap and sent < len(rows):
        texts = [r["content"] for r in rows[sent:sent + size]]
        t0 = time.time()
        status, vecs, tokens, headers, resp = ragio.voyage_embed(key, a.model, texts, "document")
        rec = {"t": t0 - t_start, "tokens": tokens, "status": status, "wait_s": 0.0, "inputs": len(texts), "chars": sum(len(x) for x in texts),
               "seconds": round(time.time() - t0, 3)}
        if status != 200:
            rec["body"] = str(resp)[:500]
            rec["headers"] = {k: v for k, v in headers.items() if k.lower().startswith(("retry-after", "x-ratelimit", "ratelimit"))}
            rec["wait_s"] = float(headers.get("retry-after") or headers.get("Retry-After") or 0)
            ledger.append(rec)
            stopped = "429" if status == 429 else "http_%s" % status
            break
        ledger.append(rec)
        sent += len(texts)
        used += tokens
        size *= 2
    ok = [r for r in ledger if r["status"] == 200]
    chars = sum(r["chars"] for r in ok)
    ratio = (sum(r["tokens"] for r in ok) / chars) if chars else None
    est_tokens = round(total_chars * ratio) if ratio else FULL_INDEX_TOKENS
    elapsed = time.time() - t_start
    last = ledger[-1] if ledger else {}
    out = {
        "model": a.model, "cap_tokens": a.cap, "stopped_by": stopped,
        "requests": len(ledger), "tokens": used, "n429": sum(1 for r in ledger if r["status"] == 429),
        "chunks_sent": sent, "seconds": round(elapsed, 1),
        "tokens_per_char": ratio, "corpus_chars": total_chars, "estimated_full_index_tokens": est_tokens,
        "per_minute": {str(m): v for m, v in evalkit.per_minute(ledger).items()},
        "records": ledger,
        "first_429": last if last.get("status") == 429 else None,
    }
    ragio.write_json(a.out, out)
    print(json_summary(out))


def json_summary(o):
    return "model=%s requests=%d tokens=%d n429=%d stopped_by=%s est_full_index_tokens=%s" % (
        o["model"], o["requests"], o["tokens"], o["n429"], o["stopped_by"], o["estimated_full_index_tokens"])


if __name__ == "__main__":
    main()
