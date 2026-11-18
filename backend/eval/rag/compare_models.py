#!/usr/bin/env python3
"""임베딩 모델 비교 (r129 EV-2) - 같은 평가 질문으로 voyage-4-lite / voyage-4 / voyage-4-large 의 Recall@5·MRR@10 (임계값 0.0), 색인 토큰·소요 시간,
최고 모델의 임계값 재탐색 (Recall@5 가 0.0 일 때보다 1점 넘게 떨어지지 않고 결과 0건 질문이 없는 가장 높은 값)
 - 말뭉치 벡터는 원본 DB opener_vec_gold 를 clone_db.sh 로 복사한 scratch DB 에서만 읽는다 (원본에는 쓰지 않는다), 끝나면 drop_db.sh 로 지운다
 - 질문 임베딩(input_type query)은 모델별 캐시(R/measure/rag-eval/query-embeddings-<모델>.json)를 쓰고 없는 것만 부른다
 - 검색은 정확 탐색(인덱스 없이 전체 비교, ORDER BY 코사인 거리 LIMIT 10)이라 인덱스 근사 오차가 섞이지 않는다 (HNSW 와의 차이는 index_eval.py)
사용: compare_models.py   (cpu-lock 안에서 실행, 키는 backend/.env)
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import evalkit  # noqa: E402
import ragio  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
EVAL = os.path.join(HERE, "retrieval_eval.jsonl")
SCRATCH = "r129_cmp"
COST_RANK = {"voyage-4-lite": 0, "voyage-4": 1, "voyage-4-large": 2}  # 동점이면 싼 쪽 (Voyage 가격표 순서)
CANDIDATES = [round(x * 0.05, 2) for x in range(0, 13)]  # 0.0 ~ 0.6


def top10(table, vec):
    lit = ragio.vec_literal(vec)
    sql = ("SELECT json_build_object('id', id, 'sim', 1 - (embedding <=> '%s'::vector)) FROM %s ORDER BY embedding <=> '%s'::vector LIMIT 10"
           % (lit, table, lit))
    return [(r["id"], r["sim"]) for r in ragio.json_rows(SCRATCH, sql)]


def main():
    items = ragio.load_eval_items(EVAL)
    gold = json.load(open(os.path.join(ragio.OUT_DIR, "gold.json")))["tables"]
    key = ragio.load_key("VOYAGE_API_KEY")
    models, runs_by_model = {}, {}
    ragio.clone_db("opener_vec_gold", SCRATCH)
    try:
        for model in ("voyage-4-lite", "voyage-4", "voyage-4-large"):
            g = gold.get(model)
            if not g or not g.get("complete"):
                raise SystemExit("gold table for %s missing or incomplete" % model)
            ledger = []
            cache, qtokens = ragio.query_embeddings(key, model, items, ledger)
            runs = [{"gold": set(it["gold_ids"]), "ranked": top10(ragio.GOLD_TABLES[model], cache[it["id"]])} for it in items]
            runs_by_model[model] = runs
            models[model] = {
                "recall_at_5": evalkit.recall_at_k(runs, 5, 0.0), "mrr_at_10": evalkit.mrr_at_k(runs, 10, 0.0),
                "tokens": g["tokens"], "index_wall_seconds": g["wall_seconds"], "index_requests": g["requests"], "index_n429": g["n429"],
                "query_tokens_new_this_run": qtokens, "queries": len(items),
                "best_similarity_median": sorted(max(s for _, s in r["ranked"]) for r in runs)[len(runs) // 2],
            }
            print(model, models[model], flush=True)
    finally:
        ragio.drop_db(SCRATCH)
    best = max(models, key=lambda m: (models[m]["recall_at_5"], -COST_RANK[m]))
    runs = runs_by_model[best]
    base = models[best]["recall_at_5"]
    sweep = [{"threshold": t, "recall_at_5": evalkit.recall_at_k(runs, 5, t), "empty_queries": evalkit.empty_count(runs, t)} for t in CANDIDATES]
    chosen_t = evalkit.pick_threshold(runs, CANDIDATES, k=5, tolerance=0.01)
    ties = [m for m in models if models[m]["recall_at_5"] == base]
    result = {
        "queries": len(items), "models": models,
        "chosen": {"model": best, "threshold": chosen_t, "app_threshold_before": 0.2,
                   "reason": "highest Recall@5 at threshold 0.0 (%.4f)%s; threshold = highest candidate whose Recall@5 stays within 1 point of that and leaves no query with 0 results"
                             % (base, ("; tie among %s resolved to the cheaper model" % ties) if len(ties) > 1 else ""),
                   "sweep": sweep},
    }
    ragio.write_json(os.path.join(ragio.OUT_DIR, "models.json"), result)
    print("chosen", result["chosen"]["model"], result["chosen"]["threshold"])


if __name__ == "__main__":
    main()
