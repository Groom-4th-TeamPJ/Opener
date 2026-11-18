#!/usr/bin/env python3
"""[asy](Asymptote 그림 소스) 블록이 RAG 검색에 주는 영향 실험 (r129 AS-1)

앱의 문서 로더(DocumentServiceImpl)는 이미 [asy] 블록을 색인 전에 걷어낸다 -> S1 vector_store 에는 [asy] 행이 없다
그래서 두 갈래를 이렇게 만든다
  stripped : S1 vector_store 복사본 그대로 (블록을 걷어낸 색인)
  with_asy : 같은 복사본에서 MATH-ko-test.jsonl 의 [asy] 가진 레코드 행을, 블록을 남긴 본문을 다시 임베딩한 행으로 바꾼 것
같은 20문항(threshold_sweep.py)을 한 번만 임베딩해 두 갈래에 쓰고, 임계값 0.0 과 앱 값에서 Hit@5 를 센다
실제 OpenAI 호출은 이 스크립트가 유일하다 (키는 backend/.env, 출력하지 않는다)

사용: asy_experiment.py --out DIR   (스크래치 DB r129_asy 를 r125s1-db 에 만들고 끝나면 지운다, S1 테이블은 쓰지 않는다)
"""
import argparse
import importlib.util
import json
import os
import re
import statistics
import subprocess
import sys
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
DOCS = os.path.join(REPO, "backend", "src", "main", "resources", "documents", "MATH-ko-test.jsonl")
APP_YML = os.path.join(REPO, "backend", "src", "main", "resources", "application-dev.yml")
ENV_FILE = "/Users/mskim/Desktop/PJ/Opener/backend/.env"
SWEEP = "/Users/mskim/Desktop/PJ/Opener/backend/report/threshold_sweep.py"
DB_CONTAINER, SRC_DB, SCRATCH_DB = "r125s1-db", "opener_s1", "r129_asy"
MODEL, TOPK, BATCH = "text-embedding-3-small", 5, 100
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
def api_key():
    with open(ENV_FILE, encoding="utf-8") as f:
        for line in f:
            if line.startswith("OPENAI_API_KEY="):
                return line.split("=", 1)[1].strip()
    raise SystemExit("OPENAI_API_KEY 부재")


class Embedder:
    """임베딩 호출 수를 입력 수와 요청 수로 따로 센다 - 상한을 넘기 전에 멈춘다"""

    def __init__(self, key):
        self.key, self.inputs, self.requests = key, 0, 0

    def embed(self, texts):
        out = []
        for i in range(0, len(texts), BATCH):
            chunk = texts[i:i + BATCH]
            if self.inputs + len(chunk) > MAX_INPUTS or self.requests + 1 > MAX_REQUESTS:
                raise SystemExit(f"임베딩 상한 초과: 입력 {self.inputs}+{len(chunk)} > {MAX_INPUTS} 또는 요청 {self.requests + 1} > {MAX_REQUESTS}")
            req = urllib.request.Request(
                "https://api.openai.com/v1/embeddings", data=json.dumps({"input": chunk, "model": MODEL}).encode(),
                headers={"Authorization": f"Bearer {self.key}", "Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=120) as r:
                data = json.loads(r.read())["data"]
            self.requests += 1
            self.inputs += len(chunk)
            out += [d["embedding"] for d in sorted(data, key=lambda d: d["index"])]
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
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)
    threshold = app_threshold()
    questions = load_questions()
    recs, unclosed = asy_records()
    embedder = Embedder(api_key())
    psql("postgres", f"DROP DATABASE IF EXISTS {SCRATCH_DB}; CREATE DATABASE {SCRATCH_DB};")
    try:
        psql(SCRATCH_DB, "CREATE EXTENSION IF NOT EXISTS vector;")
        dump = subprocess.run(["docker", "exec", DB_CONTAINER, "pg_dump", "-U", "dev", "-d", SRC_DB, "-t", "vector_store"], capture_output=True, text=True)
        if dump.returncode != 0:
            raise RuntimeError("pg_dump 실패: " + dump.stderr[:300])
        psql(SCRATCH_DB, "", stdin_sql=dump.stdout)
        corpus_before = int(psql(SCRATCH_DB, "SELECT count(*) FROM vector_store").strip())
        validation = validate_stripped(recs)

        q_emb = embedder.embed([q for q, _ in questions])
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
            "questions": len(questions), "app_threshold": threshold, "model": MODEL,
            "asy_records": len(recs), "asy_chunks": asy_rows, "unclosed_asy_blocks": unclosed,
            "with_asy_records_over_%d_chars" % LONG_CHARS: sum(1 for r in recs if len(r["with_asy"]) > LONG_CHARS),
            "corpus_rows": {"stripped": corpus_before, "with_asy": corpus_after},
            "asy_rows": {"stripped": asy_before, "with_asy": asy_rows},
            "validation": validation,
            "embedding_inputs": embedder.inputs, "embedding_requests": embedder.requests, "embedding_calls": embedder.inputs,
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
        psql("postgres", f"DROP DATABASE IF EXISTS {SCRATCH_DB} WITH (FORCE);")
        left = psql("postgres", f"SELECT count(*) FROM pg_database WHERE datname = '{SCRATCH_DB}'").strip()
        print(f"[asy] 스크래치 DB 남음: {left}", file=sys.stderr)


if __name__ == "__main__":
    main()
