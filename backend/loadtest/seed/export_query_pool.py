#!/usr/bin/env python3
"""템플릿 DB 의 저장된 청크 벡터 N 개를 모의 서버의 질문 벡터 풀 파일로 내보낸다 (r129 S1)
사용: export_query_pool.py <컨테이너> <DB> <출력 JSON> [N=300]
 - 질문 임베딩이 이 풀의 한 벡터 근처가 되므로 RAG 검색이 임계값 위 결과를 반드시 찾는다 (실제 시스템에서 질문은 같은 주제의 청크와 가깝다)
 - 행 선택은 id 의 md5 순서라 같은 DB 면 같은 풀
"""
import json
import subprocess
import sys

container, db, out = sys.argv[1], sys.argv[2], sys.argv[3]
n = int(sys.argv[4]) if len(sys.argv) > 4 else 300
sql = f"SELECT embedding::text FROM vector_store ORDER BY md5(id::text) LIMIT {n}"
p = subprocess.run(["docker", "exec", "-i", container, "psql", "-U", "dev", "-d", db, "-v", "ON_ERROR_STOP=1", "-qAt", "-c", sql], capture_output=True, text=True)
if p.returncode != 0:
    sys.exit("psql failed: " + p.stderr[:300])
vectors = [json.loads(line) for line in p.stdout.splitlines() if line.startswith("[")]
if not vectors:
    sys.exit("vector_store is empty in " + db)
with open(out, "w", encoding="utf-8") as f:
    json.dump({"source_db": db, "dimension": len(vectors[0]), "vectors": vectors}, f)
print(f"pool: {len(vectors)} vectors x {len(vectors[0])} -> {out}")
