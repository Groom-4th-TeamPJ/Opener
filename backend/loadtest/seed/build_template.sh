#!/bin/bash
# 시드 템플릿 DB(opener_tpl)를 한 번 만든다: 빈 DB -> 앱 기동(Flyway + RAG 문서 적재, 임베딩은 모의 서버) -> 종료 -> seed.sql
# 사용: build_template.sh <체크아웃 경로> [작업 폴더]   (스택은 stack.sh up 으로 먼저 올려 둔다)
set -e
source "$(dirname "${BASH_SOURCE[0]}")/../s1/lib.sh"
load_ports
CHECKOUT="${1:?체크아웃 경로}"
RUN="${2:-/tmp/r125-s1-template-build}"
mkdir -p "$RUN"
trap 'stop_pid "$RUN/app.pid"; stop_pid "$RUN/mock.pid"' EXIT

psql_db -d postgres -c "DROP DATABASE IF EXISTS opener_tpl WITH (FORCE)" -c "CREATE DATABASE opener_tpl"
JAR=$(build_jar "$CHECKOUT")
start_mock "$RUN"
start_app "$JAR" opener_tpl "$RUN"
wait_health 300
# 문서 적재는 ApplicationRunner 라 헬스 UP 과 별개 - 로그의 완료 줄을 기다린다
for _ in $(seq 1 900); do
  grep -q "Vector store initialized successfully\|already contains" "$RUN/app.log" && break
  sleep 2
done
grep "Vector store initialized\|already contains\|Loaded .* documents" "$RUN/app.log" | tail -5
stop_pid "$RUN/app.pid"
stop_pid "$RUN/mock.pid"
psql_db -d opener_tpl -f - < "$LOADTEST_DIR/seed/seed.sql"
psql_db -d opener_tpl -At -c "select 'vector_store', count(*) from vector_store union all select 'users', count(*) from users union all select 'exam_results', count(*) from exam_results union all select 'question_results', count(*) from question_results"
