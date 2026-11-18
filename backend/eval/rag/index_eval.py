#!/usr/bin/env python3
"""HNSW 대 정확 탐색 (r129 IX-1) - 고른 모델의 말뭉치에 앱 인덱스 정의(V20: HNSW m=16 ef_construction=64 코사인)와 V19 함수 비용(100)을 붙인 복사본에서
평가 질문마다 HNSW 상위 5건이 정확 탐색 상위 5건을 얼마나 찾는지(hnsw.ef_search 40, Recall 이 0.99 미만이면 100 도), 질의 시간 p50/p95, 앱 모양 질의의 계획을 잰다
 - 원본 opener_vec_gold 는 clone_db.sh 로 복사한 scratch DB 에서만 쓴다, 끝나면 drop_db.sh 로 지운다. 질문 임베딩은 compare_models.py 가 만든 캐시를 쓴다 (API 호출 없음)
 - 시간은 EXPLAIN (ANALYZE) 의 서버 실행 시간이다 (docker exec 왕복을 뺀다), 질의마다 한 번 데운 뒤 잰다
사용: index_eval.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import evalkit  # noqa: E402
import ragio  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
SCRATCH = "r129_ix"
EXACT_OFF = "SET enable_indexscan=off; SET enable_bitmapscan=off;"


def ids(sql_prefix, lit, limit=5):
    sql = "%s SELECT json_build_object('id', id) FROM vector_store ORDER BY embedding <=> '%s'::vector LIMIT %d" % (sql_prefix, lit, limit)
    return [r["id"] for r in ragio.json_rows(SCRATCH, sql)]


def exec_ms(sql_prefix, lit):
    """같은 질의를 두 번 돌려 두 번째의 서버 실행 시간(ms)"""
    q = "SELECT id FROM vector_store ORDER BY embedding <=> '%s'::vector LIMIT 5" % lit
    out = ragio.psql(SCRATCH, "%s EXPLAIN (ANALYZE, FORMAT JSON) %s; " % (sql_prefix, q) * 2)
    plans = [json.loads(chunk)[0] for chunk in split_json_arrays(out)]
    return plans[-1]["Execution Time"]


def split_json_arrays(text):
    """psql 이 EXPLAIN JSON 을 여러 줄 배열로 연달아 내보낸다 - 각 배열을 따로 떼어낸다"""
    dec, i, out = json.JSONDecoder(), 0, []
    while i < len(text):
        if text[i] == "[":
            obj, end = dec.raw_decode(text, i)
            out.append(json.dumps(obj))
            i = end
        else:
            i += 1
    return out


def node_types(plan):
    found = [plan["Node Type"]]
    for child in plan.get("Plans", []):
        found += node_types(child)
    return found


def main():
    m = json.load(open(os.path.join(ragio.OUT_DIR, "models.json")))
    model, threshold = m["chosen"]["model"], m["chosen"]["threshold"]
    items = ragio.load_eval_items(os.path.join(HERE, "retrieval_eval.jsonl"))
    cache = json.load(open(os.path.join(ragio.OUT_DIR, "query-embeddings-%s.json" % model)))
    table = ragio.GOLD_TABLES[model]
    ragio.clone_db("opener_vec_gold", SCRATCH)
    try:
        for t in set(ragio.GOLD_TABLES.values()) - {table}:
            ragio.psql(SCRATCH, "DROP TABLE IF EXISTS %s" % t)
        ragio.psql(SCRATCH, "ALTER TABLE %s RENAME TO vector_store; ALTER FUNCTION cosine_distance(vector, vector) COST 100; "
                            "CREATE INDEX vector_store_embedding_idx ON vector_store USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64); "
                            "ANALYZE vector_store;" % table)
        lits = [ragio.vec_literal(cache[it["id"]]) for it in items]
        exact = [ids(EXACT_OFF, lit) for lit in lits]
        recall, per_ef_ms = {}, {}
        efs = [40]
        for ef in efs:
            prefix = "SET hnsw.ef_search=%d;" % ef
            overlaps = [evalkit.overlap_at_k(ids(prefix, lit), ex, 5) for lit, ex in zip(lits, exact)]
            recall[str(ef)] = sum(overlaps) / len(overlaps)
            per_ef_ms[ef] = [exec_ms(prefix, lit) for lit in lits]
            if ef == 40 and recall["40"] < 0.99:
                efs.append(100)
        recall.setdefault("100", None)
        exact_ms = [exec_ms(EXACT_OFF, lit) for lit in lits]
        # 앱 모양 질의: Spring AI PgVectorStore 의 `embedding <=> q < 거리 임계 ORDER BY distance LIMIT topK`, 임계는 재탐색한 유사도 임계값
        lit0 = lits[0]
        app_q = ("EXPLAIN (FORMAT JSON) SELECT id, content, metadata, embedding <=> '%s'::vector AS distance FROM vector_store "
                 "WHERE embedding <=> '%s'::vector < %f ORDER BY distance LIMIT 5" % (lit0, lit0, 1 - threshold))
        plan = json.loads(split_json_arrays(ragio.psql(SCRATCH, "SET hnsw.ef_search=40; " + app_q))[0])[0]["Plan"]
        nodes = node_types(plan)
        hnsw_ms = per_ef_ms[40]
        out = {
            "model": model, "queries": len(items), "ef_search_tested": efs, "recall_at_5": recall,
            "latency_ms": {"hnsw": {"ef_search": 40, "p50": evalkit.percentile(hnsw_ms, 50), "p95": evalkit.percentile(hnsw_ms, 95)},
                           "exact": {"p50": evalkit.percentile(exact_ms, 50), "p95": evalkit.percentile(exact_ms, 95)}},
            "plan": "Index Scan" if "Index Scan" in nodes else nodes[0], "plan_nodes": nodes, "plan_similarity_threshold": threshold,
            "corpus_rows": int(ragio.psql(SCRATCH, "SELECT count(*) FROM vector_store").strip()),
        }
        if 100 in per_ef_ms:
            out["latency_ms"]["hnsw_ef100"] = {"ef_search": 100, "p50": evalkit.percentile(per_ef_ms[100], 50), "p95": evalkit.percentile(per_ef_ms[100], 95)}
        ragio.write_json(os.path.join(ragio.OUT_DIR, "index.json"), out)
        print(json.dumps(out, ensure_ascii=False))
    finally:
        ragio.drop_db(SCRATCH)


if __name__ == "__main__":
    main()
