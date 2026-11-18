#!/bin/bash
# 템플릿 DB 에서 복사본 DB 를 만든다 (CREATE DATABASE <target> TEMPLATE <template>) - 임베딩·시드를 다시 만들지 않고 몇 초 만에 같은 상태를 얻는다
# 사용: clone_db.sh <컨테이너> <템플릿 DB> <복사본 DB>
#  - 복사본 이름이 템플릿과 같거나 원본으로 쓰는 DB(opener_tpl, opener_vec_gold)·시스템 DB 이름이면 거부한다 (원본을 덮지 않게)
#  - 끝나면 복사본의 public 테이블별 행 수를 `테이블=행수` 로 한 줄씩 출력한다
#  - 템플릿에 다른 연결이 있으면 Postgres 가 복사를 거부한다 (오류를 그대로 보여준다)
set -euo pipefail
C="${1:?컨테이너}"; TPL="${2:?템플릿 DB}"; TARGET="${3:?복사본 DB}"
for n in "$TPL" "$TARGET"; do
  [[ "$n" =~ ^[a-z0-9_]+$ ]] || { echo "clone_db: 이름은 소문자·숫자·밑줄만 ($n)" >&2; exit 2; }
done
case "$TARGET" in
  "$TPL"|opener_tpl|opener_vec_gold|postgres|template0|template1)
    echo "clone_db: 복사본 이름으로 쓸 수 없다 ($TARGET) - 원본이거나 시스템 DB" >&2; exit 2 ;;
esac
psql_c() { docker exec -i "$C" psql -U dev -v ON_ERROR_STOP=1 "$@"; }
psql_c -d postgres -qc "DROP DATABASE IF EXISTS $TARGET WITH (FORCE)" -c "CREATE DATABASE $TARGET TEMPLATE $TPL"
psql_c -d "$TARGET" -At -c "SELECT table_name || '=' || (xpath('/row/c/text()', query_to_xml(format('SELECT count(*) AS c FROM %I', table_name), false, true, '')))[1]::text FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' ORDER BY 1"
