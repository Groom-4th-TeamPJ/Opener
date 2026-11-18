#!/bin/bash
# 시드 템플릿 DB(opener_tpl)를 한 번 만든다: 빈 DB -> 앱 기동(Flyway V20 까지 + 기동 색인, 임베딩은 모의 Voyage) -> 종료 -> seed.sql
# R129-11: S1 회차는 Voyage 를 기다리지 않게 모의 벡터로 채운다. 진짜 벡터는 eval/rag/restore_vectors.sh 로 나중에 얹고 덤프를 다시 뜬다
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
# r129 전환 이후 문서 색인은 백그라운드 가상 스레드(rag-index)라 헬스 UP 과 별개 - 로그의 [RAG-INDEX] 종료·실패 줄을 기다린다 (모의 Voyage 가 답한다)
for _ in $(seq 1 900); do
  grep -q "\[RAG-INDEX\] 색인 종료\|\[RAG-INDEX\] 색인 실패" "$RUN/app.log" && break
  sleep 2
done
grep "\[RAG-INDEX\]" "$RUN/app.log" | tail -3
grep -q "\[RAG-INDEX\] 색인 종료" "$RUN/app.log" || { echo "색인이 끝나지 않았다" >&2; exit 1; }
stop_pid "$RUN/app.pid"
stop_pid "$RUN/mock.pid"
psql_db -d opener_tpl -f - < "$LOADTEST_DIR/seed/seed.sql"
psql_db -d opener_tpl -At -c "select 'vector_store', count(*) from vector_store union all select 'users', count(*) from users union all select 'exam_results', count(*) from exam_results union all select 'question_results', count(*) from question_results"

# 템플릿의 테이블별 행 수와 덤프(pg_dump -Fc) - R129-10
OUT_DIR="${S1_TPL_OUT:-/Users/mskim/Desktop/PJ/Opener/.cascade/r129/measure/s1}"
mkdir -p "$OUT_DIR"
bash "$LOADTEST_DIR/seed/template_report.sh" r125s1-db opener_tpl "$OUT_DIR"
