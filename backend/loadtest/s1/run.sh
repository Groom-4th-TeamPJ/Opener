#!/bin/bash
# 사용: run.sh <code: base|new> <체크아웃 경로> <결과 폴더> [tiny]
# 한 회차: 스택 reset -> bootJar -> 모의 서버·앱 기동 -> 생성기·수집기 -> 앱 정지(PID 만) -> summary.json
# 측정 회차는 cpu-lock.sh 로 감싸서 부른다 (tiny 는 잠금 없이)
set -u
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
load_ports

CODE="${1:?code base|new}"; CHECKOUT="${2:?체크아웃 경로}"; OUT="${3:?결과 폴더}"; TINY="${4:-}"
TINY_FLAG=""; [ "$TINY" = "tiny" ] && TINY_FLAG="--tiny"
mkdir -p "$OUT"
COMMIT=$(git -C "$CHECKOUT" log -1 --format=%h)
cleanup() { stop_pid "$OUT/app.pid"; stop_pid "$OUT/mock.pid"; }
trap cleanup EXIT

echo "[run] code=$CODE commit=$COMMIT out=$OUT tiny=${TINY:-no}"
bash "$S1_DIR/stack.sh" reset || exit 1
psql_db -d opener_s1 -At -f - < "$LOADTEST_DIR/seed/users_json.sql" > "$OUT/users.json" || exit 1

JAR=$(build_jar "$CHECKOUT") || exit 1
start_mock "$OUT" || { echo "mock start failed" >&2; exit 1; }
start_app "$JAR" opener_s1 "$OUT" || exit 1
wait_health 300 || exit 1
# 앱이 뜬 직후의 문서 확인 검색(임베딩 호출 1회)과 큐 정리를 지난 뒤 모의 서버 집계를 0 으로 맞춘다
sleep 5
curl -s -X POST "http://localhost:$MOCK_PORT/stats/reset" >/dev/null
rm -f "$OUT"/turns-*.jsonl "$OUT"/sessions-*.jsonl "$OUT"/errors-*.jsonl "$OUT/window.json" "$OUT/collector.json" "$OUT/gen-pids.txt"

ulimit -n 65536 2>/dev/null || ulimit -n "$(ulimit -Hn)"
python3 "$LOADTEST_DIR/s1_load.py" --users "$OUT/users.json" --out "$OUT" $TINY_FLAG > "$OUT/loadgen.log" 2>&1 &
GEN=$!
python3 "$LOADTEST_DIR/collect_s1.py" watch --out "$OUT" > "$OUT/collector.log" 2>&1 &
COL=$!
wait $GEN; GEN_RC=$?
wait $COL; COL_RC=$?
echo "[run] generator exit=$GEN_RC collector exit=$COL_RC"

# 생성기가 끝난 뒤에도 저장 이벤트 소비가 남아 있으면 잠시 기다린다 (큐가 비는 시점 기록용)
python3 "$LOADTEST_DIR/collect_s1.py" summarize --out "$OUT" --code "$CODE" --commit "$COMMIT" || exit 1
grep -c "" "$OUT/app.log" | sed 's/^/[run] app.log lines: /'
echo "[run] summary: $OUT/summary.json"
