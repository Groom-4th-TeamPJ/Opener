#!/usr/bin/env python3
"""답 평가 (r129 EV-3) - ko_math-500 문제 N 개를 Claude Sonnet 5.5 가 풀게 한다: RAG 켬(고른 모델의 상위 5 청크를 앱의 RAG 프롬프트 모양으로 시스템 프롬프트에 넣음) 대 끔
최종 \\boxed{} 답을 공백 제거·\\left \\right 제거·\\dfrac->\\frac 로 맞춰 정답과 비교한다
 - ko_math-500 은 색인(MATH-ko-test 5,000건)에 번역본이 이미 들어 있을 수 있다 (MATH-500 은 MATH 테스트의 부분집합) -> 검색 결과에서 같은 문제로 판정된 청크는
   뺀다(evalkit.leaks, 수식 조각 겹침 0.6 이상). 빼기 전후 수를 기록해 누출이 정확도를 부풀리지 않았는지 본다
 - 검색은 앱처럼 유사도 임계값(models.json 의 chosen.threshold) 이상인 상위 5건, 없으면 RAG 없는 프롬프트와 같다. 질문 벡터는 캐시 파일(input_type query)을 쓴다
 - 비용 상한: queries-cost.json 의 비용과 합쳐 $6 (넘기 전에 멈춘다)
사용: answer_eval.py [--n 100] [--seed 129]   (cpu-lock 안에서 실행, 키는 backend/.env)
"""
import argparse
import concurrent.futures
import json
import os
import sys
import threading

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import evalkit  # noqa: E402
import ragio  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
DOCS = os.path.join(os.path.dirname(os.path.dirname(HERE)), "src", "main", "resources", "documents", "ko_math-500.jsonl")
MODEL, EFFORT, MAX_TOKENS = "claude-sonnet-5-5", "low", 4000
PRICE_IN, PRICE_OUT = 2.0, 10.0
CAP_USD = 6.0
SCRATCH = "r129_ans"
SEPARATOR = "\n\n=== 참고 자료 구분 ===\n\n"  # RagLlmService 와 같은 구분자
RAG_TEMPLATE = ("다음은 사용자의 질문과 관련된 참고 자료입니다:\n\n{ctx}\n\n위 참고 자료를 바탕으로 사용자의 질문에 답변해주세요.\n"
                "참고 자료에 관련 내용이 있으면 활용하되, 없다면 일반 지식으로 답변하세요.")  # prompts/chat-rag-system.txt
SOLVER = "수학 문제를 푼다. 풀이를 간단히 쓰고, 마지막에 최종 답만 \\boxed{} 안에 적는다."
LEAK_COVERAGE = 0.6
APP_THRESHOLD = 0.2  # 전환한 앱의 유사도 임계값 (application-dev.yml app.rag.similarity-threshold, R129-17: 쉬운 평가 집합에서 나온 0.55 는 앱 값이 아니다)
EASY_SET_THRESHOLD = 0.55  # EV-2 가 기록한 값 - 같은 문제들에서 검색 0건이 되는 수를 비교용으로만 센다


def retrieve(items, model, threshold, cache):
    """문제마다 정확 탐색 상위 8건에서 같은 문제 청크를 빼고 임계값 이상인 5건 -> (문맥 청크 글 목록, 뺀 수, 빼기 전 상위 5 중 누출 여부)"""
    table = ragio.GOLD_TABLES[model]
    out = {}
    ragio.clone_db("opener_vec_gold", SCRATCH)
    try:
        for it in items:
            lit = ragio.vec_literal(cache["ans-" + it["id"]])
            rows = ragio.json_rows(SCRATCH, "SELECT json_build_object('id', id, 'content', content, 'sim', 1 - (embedding <=> '%s'::vector)) FROM %s "
                                            "ORDER BY embedding <=> '%s'::vector LIMIT 8" % (lit, table, lit))
            flagged = [r for r in rows if evalkit.leaks(it["problem"], r["content"], LEAK_COVERAGE)]
            clean = [r for r in rows if r not in flagged]
            kept = [r for r in clean if r["sim"] >= threshold][:5]
            out[it["id"]] = {"chunks": [r["content"] for r in kept], "sims": [round(r["sim"], 4) for r in kept],
                             "removed": len(flagged), "top5_had_leak": any(r in flagged for r in rows[:5]),
                             "removed_ids": [r["id"] for r in flagged],
                             "chunks_at_app_threshold": len([r for r in clean if r["sim"] >= APP_THRESHOLD][:5]),
                             "chunks_at_easy_set_threshold": len([r for r in clean if r["sim"] >= EASY_SET_THRESHOLD][:5])}
    finally:
        ragio.drop_db(SCRATCH)
    return out


def solve(key, problem, context, spend, lock):
    system = SOLVER if not context else SOLVER + "\n\n" + RAG_TEMPLATE.replace("{ctx}", SEPARATOR.join(context))
    with lock:
        if spend["usd"] >= spend["cap"]:
            raise SystemExit("비용 상한 도달 $%.2f" % spend["usd"])
    text, usage, stop = ragio.anthropic_message(key, MODEL, system, problem, MAX_TOKENS, effort=EFFORT)
    with lock:
        spend["in"] += usage.get("input_tokens", 0)
        spend["out"] += usage.get("output_tokens", 0)
        spend["usd"] = evalkit.cost_usd(spend["in"], spend["out"], PRICE_IN, PRICE_OUT) + spend["prior"]
    return {"boxed": evalkit.extract_boxed(text), "stop": stop, "out_tokens": usage.get("output_tokens", 0)}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--n", type=int, default=100)
    ap.add_argument("--seed", type=int, default=129)
    a = ap.parse_args()
    models = json.load(open(os.path.join(ragio.OUT_DIR, "models.json")))
    model, threshold = models["chosen"]["model"], APP_THRESHOLD
    recs = [json.loads(l) for l in open(DOCS, encoding="utf-8") if l.strip()]
    for r in recs:
        r["type"], r["level"] = r["subject"], str(r["level"])  # 층화 키
    items = evalkit.stratified_sample(recs, a.n, a.seed)
    vkey, akey = ragio.load_key("VOYAGE_API_KEY"), ragio.load_key("ANTHROPIC_API_KEY")
    ledger = []
    cache, qtokens = ragio.query_embeddings(vkey, model, [{"id": "ans-" + it["id"], "query": it["problem"]} for it in items], ledger)
    ret = retrieve(items, model, threshold, cache)
    prior = json.load(open(os.path.join(ragio.OUT_DIR, "queries-cost.json")))["cost_usd"] if os.path.exists(os.path.join(ragio.OUT_DIR, "queries-cost.json")) else 0.0
    spend, lock = {"in": 0, "out": 0, "usd": prior, "prior": prior, "cap": CAP_USD - 0.2}, threading.Lock()

    def one(it, arm):
        ctx = ret[it["id"]]["chunks"] if arm == "rag_on" else []
        r = solve(akey, it["problem"], ctx, spend, lock)
        r.update({"id": it["id"], "arm": arm, "correct": evalkit.answers_match(r["boxed"], it["answer"]), "gold": it["answer"]})
        return r

    results = []
    with concurrent.futures.ThreadPoolExecutor(4) as pool:
        futs = [pool.submit(one, it, arm) for it in items for arm in ("rag_off", "rag_on")]
        for f in concurrent.futures.as_completed(futs):
            results.append(f.result())
    by_id = {it["id"]: it for it in items}

    def acc(rs):
        return round(sum(1 for r in rs if r["correct"]) / len(rs), 4) if rs else None

    arms = {arm: [r for r in results if r["arm"] == arm] for arm in ("rag_on", "rag_off")}
    leak_ids = {i for i, v in ret.items() if v["top5_had_leak"]}
    with_ctx = {i for i, v in ret.items() if v["chunks"]}
    out = {
        "n": len(items), "model": MODEL, "effort": EFFORT, "max_tokens": MAX_TOKENS, "retrieval_model": model, "threshold": threshold,
        "accuracy": {arm: acc(rs) for arm, rs in arms.items()},
        "correct": {arm: sum(1 for r in rs if r["correct"]) for arm, rs in arms.items()},
        "no_boxed": {arm: sum(1 for r in rs if r["boxed"] is None) for arm, rs in arms.items()},
        "truncated": {arm: sum(1 for r in rs if r["stop"] == "max_tokens") for arm, rs in arms.items()},
        "retrieval": {"problems_with_context": len(with_ctx), "zero_chunks_at_0_2": sum(1 for v in ret.values() if v["chunks_at_app_threshold"] == 0),
                      "zero_chunks_at_0_55": sum(1 for v in ret.values() if v["chunks_at_easy_set_threshold"] == 0), "problems_with_same_problem_chunk_removed": len(leak_ids),
                      "chunks_removed": sum(v["removed"] for v in ret.values()), "leak_coverage_threshold": LEAK_COVERAGE},
        "accuracy_when_context_found": {arm: acc([r for r in rs if r["id"] in with_ctx]) for arm, rs in arms.items()},
        "accuracy_when_leak_was_removed": {arm: acc([r for r in rs if r["id"] in leak_ids]) for arm, rs in arms.items()},
        "tokens": {"in": spend["in"], "out": spend["out"]}, "cost_usd": round(spend["usd"], 4), "cost_cap_usd": CAP_USD,
        "query_embedding_tokens_new": qtokens,
        "per_subject": {s: {arm: acc([r for r in rs if by_id[r["id"]]["subject"] == s]) for arm, rs in arms.items()} for s in sorted({i["subject"] for i in items})},
        "items": sorted(results, key=lambda r: (r["id"], r["arm"])),
        "retrieval_detail": {i: {k: v[k] for k in ("sims", "removed", "removed_ids", "top5_had_leak")} for i, v in ret.items()},
    }
    ragio.write_json(os.path.join(ragio.OUT_DIR, "answers.json"), out)
    print(json.dumps({k: out[k] for k in ("n", "accuracy", "correct", "no_boxed", "truncated", "retrieval", "cost_usd")}, ensure_ascii=False))


if __name__ == "__main__":
    main()
