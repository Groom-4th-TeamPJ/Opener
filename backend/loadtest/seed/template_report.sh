#!/bin/bash
# 템플릿 DB 의 테이블별 행 수를 <출력 폴더>/opener_tpl.json 에 적고 pg_dump -Fc 로 <출력 폴더>/opener_tpl.dump 를 뜬다 (R129-10)
# 사용: template_report.sh <컨테이너> <DB> <출력 폴더>
set -euo pipefail
C="${1:?컨테이너}"; DB="${2:?DB}"; OUT="${3:?출력 폴더}"
mkdir -p "$OUT"
counts=$(docker exec -i "$C" psql -U dev -d "$DB" -v ON_ERROR_STOP=1 -qAt -c "SELECT table_name || '=' || (xpath('/row/c/text()', query_to_xml(format('SELECT count(*) AS c FROM %I', table_name), false, true, '')))[1]::text FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' ORDER BY 1")
dim=$(docker exec -i "$C" psql -U dev -d "$DB" -qAt -c "SELECT atttypmod FROM pg_attribute WHERE attrelid='vector_store'::regclass AND attname='embedding'")
python3 - "$OUT/opener_tpl.json" "$DB" "$dim" <<PY
import json, sys
counts = dict(l.split("=") for l in """$counts""".splitlines() if "=" in l)
json.dump({"db": sys.argv[2], "embedding_dimension": int(sys.argv[3]), "rows": {k: int(v) for k, v in counts.items()}}, open(sys.argv[1], "w"), indent=1)
PY
docker exec "$C" pg_dump -U dev -Fc "$DB" > "$OUT/opener_tpl.dump"
echo "template_report: $OUT/opener_tpl.json $OUT/opener_tpl.dump ($(wc -c < "$OUT/opener_tpl.dump") bytes)"
