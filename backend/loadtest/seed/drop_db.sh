#!/bin/bash
# 복사본 DB 를 지운다 (DROP DATABASE ... WITH (FORCE)) - 원본(opener_tpl, opener_vec_gold)·시스템 DB 는 거부한다
# 사용: drop_db.sh <컨테이너> <지울 DB>
set -euo pipefail
C="${1:?컨테이너}"; TARGET="${2:?지울 DB}"
[[ "$TARGET" =~ ^[a-z0-9_]+$ ]] || { echo "drop_db: 이름은 소문자·숫자·밑줄만 ($TARGET)" >&2; exit 2; }
case "$TARGET" in
  opener_tpl|opener_vec_gold|postgres|template0|template1)
    echo "drop_db: 지울 수 없는 DB ($TARGET) - 원본이거나 시스템 DB" >&2; exit 2 ;;
esac
docker exec -i "$C" psql -U dev -v ON_ERROR_STOP=1 -d postgres -qc "DROP DATABASE IF EXISTS $TARGET WITH (FORCE)"
echo "drop_db: $TARGET 삭제 (없었으면 건너뜀)"
