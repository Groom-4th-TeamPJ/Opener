#!/bin/bash
# 원본(opener_vec_gold)의 한 모델 벡터를 대상 DB 의 vector_store 에 옮긴다 - 임베딩 API 를 다시 부르지 않는다
# 사용: restore_vectors.sh <컨테이너> <대상 DB> <모델 voyage-4|voyage-4-lite|voyage-4-large> [덤프 파일]
#  1. 원본 DB 가 없고 덤프 파일이 있으면 덤프(pg_dump -Fc)로 원본 DB 를 먼저 되살린다 (컨테이너가 사라진 경우)
#  2. 대상 vector_store 를 TRUNCATE -> 원본 테이블 vector_store_<모델> 에서 id·content·metadata·embedding 을 COPY (HNSW 인덱스는 내렸다가 적재 뒤 V20 정의로 다시 만든다 - 행마다 넣는 것보다 빠르다)
#  3. 행 수와 id 체크섬을 출력한다 (원본과 같아야 한다)
# 대상 DB 의 vector_store 는 Flyway V20 이후 모양(embedding vector(1024))이어야 한다
set -euo pipefail
C="${1:?컨테이너}"; DB="${2:?대상 DB}"; MODEL="${3:?모델}"
DUMP="${4:-/Users/mskim/Desktop/PJ/Opener/.cascade/r129/measure/rag-eval/vec-gold.dump}"
GOLD=opener_vec_gold
case "$MODEL" in
  voyage-4) T=vector_store_voyage_4 ;;
  voyage-4-lite) T=vector_store_voyage_4_lite ;;
  voyage-4-large) T=vector_store_voyage_4_large ;;
  *) echo "restore_vectors: 모르는 모델 ($MODEL)" >&2; exit 2 ;;
esac
[[ "$DB" =~ ^[a-z0-9_]+$ && "$DB" != "$GOLD" ]] || { echo "restore_vectors: 대상 DB 이름이 잘못됨 ($DB)" >&2; exit 2; }
pg() { docker exec -i "$C" psql -U dev -v ON_ERROR_STOP=1 "$@"; }
if [ "$(pg -d postgres -At -c "SELECT count(*) FROM pg_database WHERE datname='$GOLD'")" = 0 ]; then
  [ -f "$DUMP" ] || { echo "restore_vectors: 원본 DB 도 덤프($DUMP)도 없다" >&2; exit 1; }
  docker cp "$DUMP" "$C:/tmp/vec-gold.dump"
  pg -d postgres -qc "CREATE DATABASE $GOLD"
  docker exec "$C" pg_restore -U dev -d "$GOLD" --no-owner /tmp/vec-gold.dump
  docker exec "$C" rm -f /tmp/vec-gold.dump
fi
pg -d "$DB" -qc "DROP INDEX IF EXISTS vector_store_embedding_idx" -c "TRUNCATE TABLE vector_store"
pg -d "$GOLD" -qc "COPY (SELECT id, content, metadata, embedding FROM $T) TO STDOUT" \
  | pg -d "$DB" -qc "COPY vector_store (id, content, metadata, embedding) FROM STDIN"
pg -d "$DB" -qc "CREATE INDEX vector_store_embedding_idx ON vector_store USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64)" -c "ANALYZE vector_store"
CHK="SELECT count(*) || ' rows, id checksum ' || md5(string_agg(id::text, ',' ORDER BY id)) FROM"
echo "gold   $T: $(pg -d "$GOLD" -At -c "$CHK $T")"
echo "target $DB.vector_store: $(pg -d "$DB" -At -c "$CHK vector_store")"
