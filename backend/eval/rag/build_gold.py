#!/usr/bin/env python3
"""원본 벡터 DB 만들기 (r129 EV-2, Ruling R129-9) - 말뭉치 5,152청크를 모델마다 한 번만 임베딩해 opener_vec_gold 의 vector_store_<모델> 에 적는다
 - 청크 id·본문·메타데이터는 고정 출처 DB(ragio.SRC_DB)에서 그대로 가져온다 (검색 평가 질문의 gold id 와 같은 id)
 - 앱 V20 모양: (id uuid, content text, metadata jsonb, embedding vector(1024)), 임베딩은 input_type=document, output_dimension 1024
 - 중간에 끊겨도 이어 한다 (이미 적힌 id 는 건너뜀). 다 채운 뒤에는 이 스크립트가 그 테이블에 쓰지 않는다: 행 수가 같으면 바로 끝낸다
 - 끝나면 R/measure/rag-eval/gold.json 에 모델별 행 수·id 체크섬·토큰·소요 초를 적는다
사용: build_gold.py --model voyage-4 [--request-tokens 80000] [--max-inputs 128]   (cpu-lock 안에서 실행, 키는 backend/.env)
"""
import argparse
import csv
import fcntl
import io
import json
import os
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import evalkit  # noqa: E402
import ragio  # noqa: E402

GOLD_DB = "opener_vec_gold"
TABLES = {"voyage-4": "vector_store_voyage_4", "voyage-4-lite": "vector_store_voyage_4_lite", "voyage-4-large": "vector_store_voyage_4_large"}
GOLD_JSON = os.path.join(ragio.OUT_DIR, "gold.json")


def ensure_gold(table):
    exists = ragio.psql("postgres", "SELECT count(*) FROM pg_database WHERE datname='%s'" % GOLD_DB).strip() == "1"
    if not exists:
        ragio.psql("postgres", "CREATE DATABASE %s" % GOLD_DB)
    ragio.psql(GOLD_DB, "CREATE EXTENSION IF NOT EXISTS vector; CREATE TABLE IF NOT EXISTS %s "
                        "(id uuid PRIMARY KEY, content text NOT NULL, metadata jsonb, embedding vector(%d) NOT NULL);" % (table, ragio.DIM))


def copy_rows(table, rows, vecs):
    """CSV COPY 로 한 묶음을 적는다 - 본문의 줄바꿈·따옴표는 csv 모듈이 감싼다"""
    buf = io.StringIO()
    w = csv.writer(buf)
    for r, v in zip(rows, vecs):
        w.writerow([r["id"], r["content"], json.dumps(r["metadata"], ensure_ascii=False), "[" + ",".join(repr(x) for x in v) + "]"])
    p = subprocess.run(["docker", "exec", "-i", ragio.DB_CONTAINER, "psql", "-U", "dev", "-d", GOLD_DB, "-v", "ON_ERROR_STOP=1", "-qc",
                        "COPY %s (id, content, metadata, embedding) FROM STDIN WITH (FORMAT csv)" % table],
                       input=buf.getvalue(), capture_output=True, text=True)
    if p.returncode != 0:
        raise RuntimeError("copy failed: " + p.stderr[:400])


def update_gold_json(model, entry):
    """세 모델 프로세스가 동시에 끝날 수 있어 잠금 파일로 읽기-고치기-쓰기를 직렬화한다"""
    os.makedirs(ragio.OUT_DIR, exist_ok=True)
    with open(GOLD_JSON + ".lock", "w") as lock:
        fcntl.flock(lock, fcntl.LOCK_EX)
        cur = json.load(open(GOLD_JSON)) if os.path.exists(GOLD_JSON) else {"db": GOLD_DB, "tables": {}}
        cur["tables"][model] = entry
        ragio.write_json(GOLD_JSON, cur)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--model", required=True, choices=sorted(TABLES))
    ap.add_argument("--request-tokens", type=int, default=80_000, help="한 요청의 토큰 상한(글자 수로 어림, 1글자=1토큰)")
    ap.add_argument("--max-inputs", type=int, default=128)
    ap.add_argument("--min-interval", type=float, default=0.0, help="요청 시작 사이 최소 초 - 분당 요청 한도(무료 3회)면 20")
    a = ap.parse_args()
    table = TABLES[a.model]
    src = ragio.json_rows(ragio.SRC_DB, "SELECT json_build_object('id', id, 'content', content, 'metadata', metadata) FROM vector_store ORDER BY id")
    ensure_gold(table)
    have = {l.strip() for l in ragio.psql(GOLD_DB, "SELECT id FROM %s" % table).splitlines() if l.strip()}
    todo = [r for r in src if r["id"] not in have]
    key = ragio.load_key("VOYAGE_API_KEY")
    ledger, tokens, done, t0 = [], 0, 0, time.time()
    last_start = 0.0
    sizes = [len(r["content"]) for r in todo]
    for group in evalkit.batches(sizes, a.max_inputs, a.request_tokens):
        rows = [todo[i] for i in group]
        time.sleep(max(0.0, a.min_interval - (time.time() - last_start)))
        last_start = time.time()
        vecs, used = ragio.voyage_embed_retry(key, a.model, [r["content"] for r in rows], "document", ledger)
        copy_rows(table, rows, vecs)
        tokens += used
        done += len(rows)
        print("%s: +%d rows (%d/%d) tokens=%d" % (a.model, len(rows), len(have) + done, len(src), tokens), flush=True)
    seconds = time.time() - t0
    chk = ragio.psql(GOLD_DB, "SELECT count(*) || ' ' || md5(string_agg(id::text, ',' ORDER BY id)) FROM %s" % table).split()
    rows_now, checksum = int(chk[0]), chk[1]
    src_checksum = ragio.psql(ragio.SRC_DB, "SELECT md5(string_agg(id::text, ',' ORDER BY id)) FROM vector_store").strip()
    ok = rows_now == len(src) and checksum == src_checksum
    previous = json.load(open(GOLD_JSON))["tables"].get(a.model, {}) if os.path.exists(GOLD_JSON) else {}
    update_gold_json(a.model, {
        "table": table, "rows": rows_now, "id_checksum": checksum, "complete": ok,
        "tokens": previous.get("tokens", 0) + tokens, "wall_seconds": round(previous.get("wall_seconds", 0) + seconds, 1),
        "requests": previous.get("requests", 0) + len(ledger), "n429": previous.get("n429", 0) + sum(1 for r in ledger if r["status"] == 429),
        "resumed_rows_skipped": len(have), "dimension": ragio.DIM, "input_type": "document"})
    print("%s done: rows=%d complete=%s tokens=%d seconds=%.0f" % (a.model, rows_now, ok, tokens, seconds))
    if not ok:
        sys.exit(1)


if __name__ == "__main__":
    main()
