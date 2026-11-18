"""RAG 평가 스크립트 공통 입출력 - .env 키 읽기(출력하지 않음)·Voyage/Anthropic REST 호출·r125s1-db psql
시험하지 않는다 (순수 계산은 evalkit.py): 실제 API 와 컨테이너가 있어야 돌고, 각 실행이 결과 파일로 증명한다
"""
import json
import os
import subprocess
import time
import urllib.error
import urllib.request

ENV_FILE = "/Users/mskim/Desktop/PJ/Opener/backend/.env"
ROUND_DIR = "/Users/mskim/Desktop/PJ/Opener/.cascade/r129"
OUT_DIR = os.path.join(ROUND_DIR, "measure", "rag-eval")
DB_CONTAINER = "r125s1-db"
VOYAGE_URL = "https://api.voyageai.com/v1/embeddings"
ANTHROPIC_URL = "https://api.anthropic.com/v1/messages"
DIM = 1024


def load_key(name):
    """backend/.env 의 한 키 값 - 값은 반환만 하고 어디에도 출력하지 않는다"""
    with open(ENV_FILE, encoding="utf-8") as f:
        for line in f:
            if line.startswith(name + "="):
                v = line.split("=", 1)[1].strip().strip("'\"")
                if v:
                    return v
    raise SystemExit("key %s missing in .env" % name)


def http_json(url, body, headers, timeout=120):
    """POST -> (status, 응답 JSON 또는 본문 글, 응답 헤더) - HTTP 오류도 예외 없이 돌려준다"""
    req = urllib.request.Request(url, data=json.dumps(body).encode(), headers={"content-type": "application/json", **headers}, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.status, json.loads(r.read()), dict(r.headers)
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw), dict(e.headers)
        except ValueError:
            return e.code, raw, dict(e.headers)


def voyage_embed(key, model, texts, input_type, dim=DIM):
    """한 번 호출 -> (status, vectors 또는 None, 사용 토큰, 헤더, 본문) - 재시도는 호출한 쪽이 한다"""
    body = {"input": texts, "model": model, "output_dimension": dim, "input_type": input_type}
    status, resp, headers = http_json(VOYAGE_URL, body, {"authorization": "Bearer " + key})
    if status == 200:
        data = sorted(resp["data"], key=lambda d: d["index"])
        return status, [d["embedding"] for d in data], resp["usage"]["total_tokens"], headers, resp
    return status, None, 0, headers, resp


def voyage_embed_retry(key, model, texts, input_type, ledger, max_attempts=12):
    """429 는 Retry-After(없으면 1초에서 두 배, 60초 상한)만큼 쉬고 다시 - ledger 에 시도마다 기록 {t,tokens,status,wait_s}"""
    wait = 1.0
    for attempt in range(max_attempts):
        t0 = time.time()
        status, vecs, tokens, headers, resp = voyage_embed(key, model, texts, input_type)
        rec = {"t": t0, "tokens": tokens, "status": status, "wait_s": 0.0, "inputs": len(texts)}
        if status == 200:
            ledger.append(rec)
            return vecs, tokens
        if status != 429:
            raise RuntimeError("voyage %s: %s" % (status, str(resp)[:300]))
        ra = headers.get("retry-after") or headers.get("Retry-After")
        wait = min(float(ra) if ra else wait, 60.0)
        rec["wait_s"] = wait
        ledger.append(rec)
        time.sleep(wait)
        wait = min(wait * 2, 60.0)
    raise RuntimeError("voyage 429 after %d attempts" % max_attempts)


def anthropic_message(key, model, system, user, max_tokens, effort=None):
    """Messages API 비스트리밍 한 번 -> (본문 글, usage dict, stop_reason)"""
    body = {"model": model, "max_tokens": max_tokens, "messages": [{"role": "user", "content": user}]}
    if system:
        body["system"] = system
    if effort:
        body["output_config"] = {"effort": effort}
    headers = {"x-api-key": key, "anthropic-version": "2023-06-01"}
    for attempt in range(6):
        status, resp, h = http_json(ANTHROPIC_URL, body, headers, timeout=300)
        if status == 200:
            text = "".join(b.get("text", "") for b in resp["content"] if b.get("type") == "text")
            return text, resp["usage"], resp.get("stop_reason")
        if status in (429, 500, 502, 503, 529):
            time.sleep(min(float(h.get("retry-after", 2 ** attempt)), 60))
            continue
        raise RuntimeError("anthropic %s: %s" % (status, str(resp)[:300]))
    raise RuntimeError("anthropic retries exhausted")


def psql(db, sql, container=DB_CONTAINER, tuples=True):
    """r125s1-db 에서 SQL 한 번 -> 표준출력 글 (오류면 예외)"""
    args = ["docker", "exec", "-i", container, "psql", "-U", "dev", "-d", db, "-v", "ON_ERROR_STOP=1"] + (["-At"] if tuples else [])
    p = subprocess.run(args, input=sql, capture_output=True, text=True)
    if p.returncode != 0:
        raise RuntimeError("psql %s: %s" % (db, p.stderr.strip()[:400]))
    return p.stdout


def json_rows(db, sql):
    """SELECT 가 json 한 열을 돌려주게 쓴 쿼리 -> 파이썬 객체 목록 (한 행 한 줄)"""
    return [json.loads(line) for line in psql(db, sql).splitlines() if line.strip()]


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)
