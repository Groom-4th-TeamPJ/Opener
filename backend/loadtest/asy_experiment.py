#!/usr/bin/env python3
"""[asy](Asymptote 그림 소스) 블록이 RAG 검색에 주는 영향 실험 (r129 AS-1)

앱의 문서 로더(DocumentServiceImpl)는 이미 [asy] 블록을 색인 전에 걷어낸다 -> 색인에는 [asy] 행이 없다
그래서 두 갈래를 이렇게 만든다
  stripped : 고른 Voyage 모델의 원본 벡터(opener_vec_gold)를 복사한 그대로 (블록을 걷어낸 색인)
  with_asy : 같은 복사본에서 MATH-ko-test.jsonl 의 [asy] 가진 레코드 행을, 블록을 남긴 본문을 다시 임베딩(input_type document)한 행으로 바꾼 것
같은 20문항(threshold_sweep.py)을 질문 임베딩 캐시(input_type query)로 한 번만 임베딩해 두 갈래에 쓰고, 임계값 0.0 과 앱 값에서 Hit@5 를 센다
실제 Voyage 호출은 이 스크립트(와 eval/rag 의 색인·질문 스크립트)뿐이다 (키는 backend/.env, 출력하지 않는다)

사용: asy_experiment.py --out DIR [--request-tokens N]   (scratch DB r129_asy 는 clone_db.sh 로 원본에서 복사하고 끝나면 drop_db.sh 로 지운다, 원본·S1 테이블에는 쓰지 않는다)
"""
import argparse
import importlib.util
import json
import os
import re
import statistics
import subprocess
import sys
import time
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
DOCS = os.path.join(REPO, "backend", "src", "main", "resources", "documents", "MATH-ko-test.jsonl")
APP_YML = os.path.join(REPO, "backend", "src", "main", "resources", "application-dev.yml")
sys.path.insert(0, os.path.join(REPO, "backend", "eval", "rag"))
import evalkit  # noqa: E402
import ragio  # noqa: E402

SWEEP = "/Users/mskim/Desktop/PJ/Opener/backend/report/threshold_sweep.py"
DB_CONTAINER, SCRATCH_DB = "r125s1-db", "r129_asy"
TOPK, BATCH = 5, 100
MAX_INPUTS, MAX_REQUESTS = 1200, 100
# shortcut: 진짜 TokenTextSplitter(800 토큰)를 재현하지 않고 레코드 한 건을 행 한 건으로 임베딩한다, 아래 글자 수를 넘는 레코드는 실제 로더라면 둘 이상으로 나뉠 수 있어 개수만 보고한다 - 분할까지 맞추려면 Java 로더를 돌린다
LONG_CHARS = 1700

ASY_BLOCK = re.compile(r"\[asy\].*?\[/asy\]", re.DOTALL)
BLANK_RUN = re.compile(r"\n{3,}")


# ---------- 순수 함수 (시험 대상) ----------
def strip_asy(text):
    """DocumentServiceImpl.stripAsyBlocks 와 같은 규칙 -> (본문, 닫히지 않은 [asy] 개수)
    닫히지 않은 블록은 지우지 않고 남긴다 (끝까지 지워 문제가 사라지는 것보다 낫다는 로더의 선택), 개수로 보고한다"""
    if text is None:
        return "", 0
    if "[asy]" not in text:
        return text, 0
    stripped = BLANK_RUN.sub("\n\n", ASY_BLOCK.sub("", text)).strip()
    return stripped, stripped.count("[asy]")


def first_text(node, *keys):
    for key in keys:
        v = node.get(key)
        if isinstance(v, str) and v.strip():
            return v
    return ""


def has_asy(node):
    return "[asy]" in first_text(node, "problem_ko", "problem") or "[asy]" in first_text(node, "solution_ko", "solution")


def build_content(node, strip):
    """DocumentServiceImpl.loadJsonlDocuments 가 한 레코드에서 만드는 본문 - 본문이 비면(로더가 건너뛰는 레코드) None"""
    problem, solution = first_text(node, "problem_ko", "problem"), first_text(node, "solution_ko", "solution")
    if strip:
        problem, solution = strip_asy(problem)[0], strip_asy(solution)[0]
        if not problem.strip() and not solution.strip():
            return None
    level, kind = first_text(node, "level"), first_text(node, "type", "subject")
    return "[%s] %s\n\n## 문제\n%s\n\n## 풀이\n%s" % (kind, level, problem, solution)


def hit_at_5(rows, want, threshold):
    """rows: 유사도 내림차순 (유사도, type, asy) - 임계값 이상인 행 중 앞 5건에 기대 type 이 있으면 맞힘 (threshold_sweep.py 와 같은 정의)"""
    return any(t == want for sim, t, _ in [r for r in rows if r[0] >= threshold][:TOPK])


def hit_count(per_q, threshold):
    return sum(1 for q in per_q if hit_at_5(q["rows"], q["want"], threshold))


def best_similarity_stats(per_q):
    best = [q["rows"][0][0] for q in per_q if q["rows"]]
    return {"median": statistics.median(best), "max": max(best), "min": min(best)} if best else {"median": None, "max": None, "min": None}


def asy_share_top5(per_q):
    """질문마다 유사도 상위 5건 전부(임계값 무관) 중 [asy] 행의 수와 비율"""
    top = [r for q in per_q for r in q["rows"][:TOPK]]
    asy = sum(1 for r in top if r[2])
    return {"asy": asy, "rows": len(top), "share": asy / len(top) if top else None}


def arm_result(per_q, app_threshold):
    return {"hit_at_5_t0": hit_count(per_q, 0.0), "hit_at_5_app": hit_count(per_q, app_threshold),
            "best_similarity": best_similarity_stats(per_q), "asy_share_top5": asy_share_top5(per_q)}


# ---------- 입출력 ----------
class Embedder:
    """Voyage 호출 수를 입력 수와 요청 수(429 재시도 포함)로 따로 센다 - 상한을 넘기 전에 멈춘다"""

    def __init__(self, key, model, request_tokens, min_interval=0.0):
        self.key, self.model, self.request_tokens, self.inputs, self.ledger = key, model, request_tokens, 0, []
        self.min_interval, self.last_start = min_interval, 0.0

    @property
    def requests(self):
        """성공한 요청 수 - 429 로 거절된 시도는 토큰을 쓰지 않아 상한(요청 100)에 넣지 않고 embedding_attempts 로 따로 적는다"""
        return sum(1 for r in self.ledger if r["status"] == 200)

    def check(self, n_inputs):
        if self.inputs + n_inputs > MAX_INPUTS or self.requests + 1 > MAX_REQUESTS:
            raise SystemExit(f"임베딩 상한 초과: 입력 {self.inputs}+{n_inputs} > {MAX_INPUTS} 또는 요청 {self.requests + 1} > {MAX_REQUESTS}")

    def embed(self, texts, input_type="document"):
        out = [None] * len(texts)
        for group in evalkit.batches([len(t) for t in texts], BATCH, self.request_tokens):
            self.check(len(group))
            time.sleep(max(0.0, self.min_interval - (time.time() - self.last_start)))  # 무료 한도(분당 1만 토큰)에 맞춘 간격
            self.last_start = time.time()
            vecs, _ = ragio.voyage_embed_retry(self.key, self.model, [texts[i] for i in group], input_type, self.ledger)
            self.inputs += len(group)
            for i, v in zip(group, vecs):
                out[i] = v
        return out


def psql(db, sql, stdin_sql=None):
    cmd = ["docker", "exec", "-i", DB_CONTAINER, "psql", "-U", "dev", "-d", db, "-v", "ON_ERROR_STOP=1", "-At", "-F", "|"]
    r = subprocess.run(cmd + ["-f", "-"], input=sql if stdin_sql is None else stdin_sql, capture_output=True, text=True)
    if r.returncode != 0:
        raise RuntimeError("psql 실패: " + r.stderr.strip()[:500])
    return r.stdout


def vec(v):
    return "[" + ",".join(f"{x:.7g}" for x in v) + "]"


def top_rows(db, embedding, n=25):
    """거리 오름차순 상위 n건 (유사도, type, asy) - 색인 대신 순차 검색으로 정확한 순위를 얻는다 (질의 안 SET 은 이 연결에만 적용)"""
    lit = vec(embedding)
    out = psql(db, "SET enable_indexscan = off; SET enable_bitmapscan = off;\n"
                   f"SELECT round((1 - (embedding <=> '{lit}'))::numeric, 4), coalesce(metadata->>'type','-'), (content LIKE '%[asy]%') "
                   f"FROM vector_store ORDER BY embedding <=> '{lit}' LIMIT {n};")
    rows = [(float(a), b.strip(), c.strip() == "t") for a, b, c in (l.split("|") for l in out.splitlines() if l.count("|") == 2)]
    if not rows:
        raise RuntimeError("검색 결과 0건")
    return rows


def app_threshold():
    with open(APP_YML, encoding="utf-8") as f:
        m = re.search(r"similarity-threshold:\s*([0-9.]+)", f.read())
    if not m:
        raise SystemExit("application-dev.yml 에 similarity-threshold 부재")
    return float(m.group(1))


def load_questions():
    spec = importlib.util.spec_from_file_location("threshold_sweep", SWEEP)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)  # main 은 __main__ 일 때만 돈다
    return list(mod.QUESTIONS)


def asy_records():
    """MATH-ko-test.jsonl 에서 [asy] 를 가진 레코드 - 줄 번호는 로더처럼 빈 줄도 센 1부터의 번호, 걷어내면 본문이 비는 레코드는 로더가 건너뛰므로 뺀다"""
    recs, unclosed = [], 0
    with open(DOCS, encoding="utf-8") as f:
        for n, line in enumerate(f, 1):
            if not line.strip():
                continue
            node = json.loads(line)
            if not has_asy(node):
                continue
            stripped = build_content(node, True)
            if stripped is None:
                continue
            unclosed += strip_asy(first_text(node, "problem_ko", "problem"))[1] + strip_asy(first_text(node, "solution_ko", "solution"))[1]
            recs.append({"line": n, "with_asy": build_content(node, False), "stripped": stripped})
    return recs, unclosed


def replace_rows(recs, embeddings):
    """레코드 줄 번호로 찾은 옛 행(걷어낸 본문)을 블록을 남긴 본문 한 행으로 바꾼다 - 옛 행이 없는 레코드는 건너뛴다, 바뀐 레코드 수를 돌려준다"""
    replaced = 0
    for i in range(0, len(recs), BATCH):
        stmts = ["BEGIN;"]
        for rec, emb in zip(recs[i:i + BATCH], embeddings[i:i + BATCH]):
            tag = "$a1$"
            assert tag not in rec["with_asy"]
            where = f"metadata->>'source' = 'MATH-ko-test.jsonl' AND (metadata->>'line_number')::int = {rec['line']}"
            stmts.append(f"CREATE TEMP TABLE IF NOT EXISTS r129_old (id uuid); TRUNCATE r129_old; INSERT INTO r129_old SELECT id FROM vector_store WHERE {where};")
            stmts.append(f"INSERT INTO vector_store (content, metadata, embedding) SELECT {tag}{rec['with_asy']}{tag}, metadata, '{vec(emb)}'::vector "
                         f"FROM vector_store WHERE id IN (SELECT id FROM r129_old) ORDER BY id LIMIT 1;")
            stmts.append("DELETE FROM vector_store WHERE id IN (SELECT id FROM r129_old);")
        stmts.append("COMMIT;")
        psql(SCRATCH_DB, "\n".join(stmts))
    replaced = int(psql(SCRATCH_DB, "SELECT count(*) FROM vector_store WHERE content LIKE '%[asy]%' AND metadata->>'source' = 'MATH-ko-test.jsonl'").strip())
    return replaced


def validate_stripped(recs):
    """걷어낸 본문이 S1 복사본의 행 본문과 글자 그대로 같은 레코드 수 - 로더와 같은 본문을 만든다는 확인, 행이 둘 이상인 레코드와 다른 레코드는 따로 센다"""
    out = psql(SCRATCH_DB, "SELECT coalesce(json_agg(json_build_object('l', (metadata->>'line_number')::int, 'c', content)), '[]') "
                           "FROM vector_store WHERE metadata->>'source' = 'MATH-ko-test.jsonl'")
    rows = {}
    for r in json.loads(out):
        rows.setdefault(r["l"], []).append(r["c"])
    exact = multi = other = 0
    for r in recs:
        got = rows.get(r["line"], [])
        if len(got) > 1:
            multi += 1
        elif got and got[0] == r["stripped"]:
            exact += 1
        else:
            other += 1
    return {"records": len(recs), "row_text_equals_loader_text": exact, "records_with_several_rows": multi, "row_text_differs": other}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--request-tokens", type=int, default=80_000, help="Voyage 한 요청의 토큰 상한(글자 수로 어림), 무료 한도가 낮으면 voyage-limit.json 을 보고 낮춘다")
    ap.add_argument("--min-interval", type=float, default=0.0, help="Voyage 요청 시작 사이 최소 초 - 무료 한도에서는 요청 토큰/1만*60 초 이상")
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)
    threshold = app_threshold()
    questions = load_questions()
    recs, unclosed = asy_records()
    model = json.load(open(os.path.join(ragio.OUT_DIR, "models.json")))["chosen"]["model"]
    embedder = Embedder(ragio.load_key("VOYAGE_API_KEY"), model, args.request_tokens, args.min_interval)
    ragio.clone_db("opener_vec_gold", SCRATCH_DB)
    try:
        # 원본 테이블 vector_store_<모델> 만 남기고 앱 이름(vector_store)으로 바꾼다 - id 기본값은 앱 테이블처럼 gen_random_uuid()
        for t in set(ragio.GOLD_TABLES.values()) - {ragio.GOLD_TABLES[model]}:
            psql(SCRATCH_DB, f"DROP TABLE IF EXISTS {t}")
        psql(SCRATCH_DB, f"ALTER TABLE {ragio.GOLD_TABLES[model]} RENAME TO vector_store; ALTER TABLE vector_store ALTER COLUMN id SET DEFAULT gen_random_uuid();")
        corpus_before = int(psql(SCRATCH_DB, "SELECT count(*) FROM vector_store").strip())
        validation = validate_stripped(recs)

        cache, _ = ragio.query_embeddings(embedder.key, model, [{"id": f"asy-q{i}", "query": q} for i, (q, _) in enumerate(questions)], embedder.ledger)
        q_emb = [cache[f"asy-q{i}"] for i in range(len(questions))]
        per_q = {"stripped": [], "with_asy": []}
        for (q, want), e in zip(questions, q_emb):
            per_q["stripped"].append({"q": q, "want": want, "rows": top_rows(SCRATCH_DB, e)})
        asy_before = int(psql(SCRATCH_DB, "SELECT count(*) FROM vector_store WHERE content LIKE '%[asy]%'").strip())

        doc_emb = embedder.embed([r["with_asy"] for r in recs])
        asy_rows = replace_rows(recs, doc_emb)
        corpus_after = int(psql(SCRATCH_DB, "SELECT count(*) FROM vector_store").strip())
        for (q, want), e in zip(questions, q_emb):
            per_q["with_asy"].append({"q": q, "want": want, "rows": top_rows(SCRATCH_DB, e)})
        arms = {k: arm_result(v, threshold) for k, v in per_q.items()}
        result = {
            "questions": len(questions), "app_threshold": threshold, "model": model,
            "asy_records": len(recs), "asy_chunks": asy_rows, "unclosed_asy_blocks": unclosed,
            "with_asy_records_over_%d_chars" % LONG_CHARS: sum(1 for r in recs if len(r["with_asy"]) > LONG_CHARS),
            "corpus_rows": {"stripped": corpus_before, "with_asy": corpus_after},
            "asy_rows": {"stripped": asy_before, "with_asy": asy_rows},
            "validation": validation,
            "embedding_inputs": embedder.inputs, "embedding_requests": embedder.requests, "embedding_calls": embedder.inputs, "embedding_429": sum(1 for r in embedder.ledger if r["status"] == 429), "embedding_attempts_incl_429": len(embedder.ledger),
            **arms,
            "hit_flips": [{"q": a["q"], "want": a["want"], "stripped": [hit_at_5(a["rows"], a["want"], t) for t in (0.0, threshold)],
                           "with_asy": [hit_at_5(b["rows"], b["want"], t) for t in (0.0, threshold)]}
                          for a, b in zip(per_q["stripped"], per_q["with_asy"])
                          if [hit_at_5(a["rows"], a["want"], t) for t in (0.0, threshold)] != [hit_at_5(b["rows"], b["want"], t) for t in (0.0, threshold)]],
            "per_question": [{"q": a["q"], "want": a["want"], "stripped_top5": a["rows"][:TOPK], "with_asy_top5": b["rows"][:TOPK]}
                             for a, b in zip(per_q["stripped"], per_q["with_asy"])],
        }
        with open(os.path.join(args.out, "result.json"), "w", encoding="utf-8") as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        print(json.dumps({k: result[k] for k in ("asy_chunks", "corpus_rows", "stripped", "with_asy", "embedding_inputs", "embedding_requests")}, ensure_ascii=False, indent=1))
    finally:
        ragio.drop_db(SCRATCH_DB)
        left = psql("postgres", f"SELECT count(*) FROM pg_database WHERE datname = '{SCRATCH_DB}'").strip()
        print(f"[asy] 스크래치 DB 남음: {left}", file=sys.stderr)


if __name__ == "__main__":
    main()
