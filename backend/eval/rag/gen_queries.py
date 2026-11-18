#!/usr/bin/env python3
"""검색 평가 질문 생성 (r129 EV-1) - 색인된 MATH 문제를 type x level 로 층화 추출해, 문제마다 Claude Sonnet 5.5 가
학생 말투 한국어 질문 한 건을 쓴다 (문제를 베끼지 않고 그 문제의 도움을 구하는 질문). 정답 = 그 문제의 청크 id 전부
사용: gen_queries.py [--n 150] [--seed 129]   (cpu-lock 안에서 실행, 키는 backend/.env, 이미 쓴 항목은 건너뛴다)
입력: r125s1-db 의 ragio.SRC_DB 의 vector_store (청크 id·본문·메타데이터만 읽는다, 벡터는 쓰지 않는다)
출력: backend/eval/rag/retrieval_eval.jsonl · R/measure/rag-eval/queries-cost.json
"""
import argparse
import concurrent.futures
import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import evalkit  # noqa: E402
import ragio  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "retrieval_eval.jsonl")
MODEL = "claude-sonnet-5-5"
TYPES = {"Algebra", "Intermediate Algebra", "Prealgebra", "Precalculus", "Number Theory", "Geometry", "Counting & Probability"}
PRICE_IN, PRICE_OUT = 2.0, 10.0  # Claude Sonnet 5.5 $/1M 토큰 (claude-api 스킬 모델 표, 2026-11-03)
MAX_ATTEMPTS = 5

SYSTEM = ("너는 수학 튜터링 서비스에 질문을 올리는 한국 학생이다. 주어진 수학 문제 하나를 풀다가 막혀서 튜터에게 도움을 구하는 질문 한 건을 쓴다.\n"
          "규칙: 한국어 구어체 1~3문장. 문제 문장을 그대로 옮기지 말고 자기 말로 바꿔 쓴다(문제 속 문장을 30글자 이상 연달아 베끼면 안 된다). "
          "무엇을 구해야 하는지 또는 어디서 막혔는지가 드러나야 한다. 정답이나 풀이는 쓰지 않는다. 질문 문장만 출력하고 따옴표·머리말은 붙이지 않는다.")
RETRY_NOTE = "\n(이전 시도는 문제 문장을 너무 많이 베꼈다. 이번에는 숫자와 수식 말고는 문제 문장을 쓰지 말고 완전히 다른 표현으로 써라.)"


def candidates():
    """문제(parent_document_id)당 한 행: 청크 id 목록(순서대로)과 본문 합본, 첫 청크의 문제 본문"""
    sql = ("SELECT json_build_object('parent', metadata->>'parent_document_id', 'type', metadata->>'type', 'level', metadata->>'level', "
           "'ids', json_agg(id ORDER BY (metadata->>'chunk_index')::int), 'text', string_agg(content, E'\\n' ORDER BY (metadata->>'chunk_index')::int)) "
           "FROM vector_store WHERE metadata->>'source'='MATH-ko-test.jsonl' AND metadata->>'type' IS NOT NULL "
           "GROUP BY 1, 2, 3")
    rows = [r for r in ragio.json_rows(ragio.SRC_DB, sql) if r["type"] in TYPES]
    for r in rows:
        r["id"] = r["parent"]
    return rows


def problem_text(full):
    m = re.search(r"## 문제\n(.*?)\n\n## 풀이", full, re.DOTALL)
    return (m.group(1) if m else full[:800]).strip()


def write_query(key, row, usage_total):
    base = "수학 분야: %s / 난이도: %s\n\n[문제]\n%s" % (row["type"], row["level"], problem_text(row["text"]))
    for attempt in range(MAX_ATTEMPTS):
        text, usage, stop = ragio.anthropic_message(key, MODEL, SYSTEM, base + (RETRY_NOTE if attempt else ""), 400, effort="low")
        usage_total["in"] += usage.get("input_tokens", 0)
        usage_total["out"] += usage.get("output_tokens", 0)
        q = text.strip().strip('"').strip()
        if not q or stop == "max_tokens":
            usage_total["empty_or_cut"] += 1
            continue
        if evalkit.shares_run(q, row["text"], 30):
            usage_total["copied_rejected"] += 1
            continue
        return q, attempt + 1
    return None, MAX_ATTEMPTS


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--n", type=int, default=150)
    ap.add_argument("--seed", type=int, default=129)
    a = ap.parse_args()
    key = ragio.load_key("ANTHROPIC_API_KEY")
    rows = candidates()
    sample = evalkit.stratified_sample(rows, a.n, a.seed)
    done = {}
    if os.path.exists(OUT):
        with open(OUT, encoding="utf-8") as f:
            done = {json.loads(l)["id"]: json.loads(l) for l in f if l.strip()}
    usage = {"in": 0, "out": 0, "empty_or_cut": 0, "copied_rejected": 0, "gave_up": 0}
    todo = [r for r in sample if r["id"] not in done]
    with concurrent.futures.ThreadPoolExecutor(4) as pool:
        futs = {pool.submit(write_query, key, r, usage): r for r in todo}
        for fut in concurrent.futures.as_completed(futs):
            r = futs[fut]
            q, attempts = fut.result()
            if q is None:
                usage["gave_up"] += 1
                continue
            done[r["id"]] = {"id": r["id"], "query": q, "gold_ids": r["ids"], "type": r["type"], "level": r["level"], "attempts": attempts}
    order = {r["id"]: i for i, r in enumerate(sample)}
    items = sorted(done.values(), key=lambda x: order.get(x["id"], 1 << 30))
    with open(OUT, "w", encoding="utf-8") as f:
        for it in items:
            f.write(json.dumps(it, ensure_ascii=False) + "\n")
    cost = evalkit.cost_usd(usage["in"], usage["out"], PRICE_IN, PRICE_OUT)
    ragio.write_json(os.path.join(ragio.OUT_DIR, "queries-cost.json"),
                     {"model": MODEL, "effort": "low", "max_tokens": 400, "requested": len(sample), "written": len(items), **usage, "cost_usd": round(cost, 4)})
    print("written=%d requested=%d gave_up=%d copied_rejected=%d cost_usd=%.4f" % (len(items), len(sample), usage["gave_up"], usage["copied_rejected"], cost))


if __name__ == "__main__":
    main()
