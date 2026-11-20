#!/bin/bash
# 사용: [S1_THREAD_DUMP=초] [S1_REFUSE=N] [S1_APP_ENV="K=V K2=V2"] [S1_APP_COMMIT=<해시>] [S1_FAULT=1 [S1_FAULT_KIND=mock|mock-embeddings|master-stop|cluster-pause]] run.sh <code: base|new|A|AB> <체크아웃 경로> <결과 폴더> [tiny]
# 한 회차: 스택 reset -> bootJar -> 모의 서버·앱 기동 -> 생성기·수집기 -> 큐 드레인 표집(최대 300초)·DB 행 읽기 -> 앱 정지(PID 만) -> summary.json
# 측정 회차는 cpu-lock.sh 로 감싸서 부른다 (tiny 는 잠금 없이)
set -u
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
load_ports

CODE="${1:?code base|new}"; CHECKOUT="${2:?체크아웃 경로}"; OUT="${3:?결과 폴더}"; TINY="${4:-}"
TINY_FLAG=""; [ "$TINY" = "tiny" ] && TINY_FLAG="--tiny"
mkdir -p "$OUT"
# S1_APP_COMMIT: 측정 도구만 커밋이 늘어난 체크아웃에서 앱 코드의 기준 커밋을 요약에 적는다 (backend/src 가 같다는 확인은 실행하는 쪽 몫)
COMMIT="${S1_APP_COMMIT:-$(git -C "$CHECKOUT" log -1 --format=%h)}"
# 채팅 클러스터를 멈추는 장애 종류가 중간에 끊겨도 노드를 되돌려 다음 회차가 멈춘 노드를 만나지 않게 한다
cleanup() { stop_pid "$OUT/app.pid"; stop_pid "$OUT/mock.pid"; [ -n "${S1_FAULT_KIND:-}" ] && heal_chat_cluster; }
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
rm -f "$OUT/summary.json" "$OUT"/turns-*.jsonl "$OUT"/sessions-*.jsonl "$OUT"/errors-*.jsonl "$OUT/window.json" "$OUT/collector.json" "$OUT/gen-pids.txt" "$OUT/circuit.json"

# 부하 없는 Redis CPU 바탕값 20초 (앱이 떠 있는 상태, 연결 풀 생성이 끝난 뒤)
rm -f "$OUT/idle.json"
python3 "$LOADTEST_DIR/collect_s1.py" idle --scenario "$SCENARIO" --out "$OUT" --seconds 20 || echo "[run] idle baseline failed" >&2
curl -s -X POST "http://localhost:$MOCK_PORT/stats/reset" >/dev/null
# S1_REFUSE=N: 모의 서버가 다음 N 건의 스트림을 본문 2조각 뒤 stop_reason refusal 로 끝낸다 (tiny 에서 앱이 이를 LLM 오류 이벤트로 바꾸는지 끝에서 끝까지 본다, 본문은 turns-*.jsonl 의 error_detail)
[ -n "${S1_REFUSE:-}" ] && curl -s -X POST "http://localhost:$MOCK_PORT/refuse" -d "{\"next\": $S1_REFUSE, \"text_chunks\": 2}" >/dev/null

ulimit -n 65536 2>/dev/null || ulimit -n "$(ulimit -Hn)"
python3 "$LOADTEST_DIR/s1_load.py" --scenario "$SCENARIO" --users "$OUT/users.json" --out "$OUT" $TINY_FLAG > "$OUT/loadgen.log" 2>&1 &
GEN=$!
python3 "$LOADTEST_DIR/collect_s1.py" watch --scenario "$SCENARIO" --out "$OUT" > "$OUT/collector.log" 2>&1 &
COL=$!
# S1_PS_SAMPLE=<간격 초>: 호스트 프로세스별 CPU 상위 12개를 그 간격으로 $OUT/ps-top.txt 에 적는다 (진단용)
[ -n "${S1_PS_SAMPLE:-}" ] && bash "$S1_DIR/pssample.sh" "$OUT/ps-top.txt" "$S1_PS_SAMPLE" "$GEN" > /dev/null 2>&1 &
# S1_THREAD_DUMP=<간격 초>: 생성기가 도는 동안 앱 스레드 덤프를 그 간격으로 뜬다 (진단용, 시작 120초 뒤부터)
TD_PID=""
if [ -n "${S1_THREAD_DUMP:-}" ]; then
  bash "$S1_DIR/threaddump.sh" "$OUT/app.pid" "$OUT/threads" "$S1_THREAD_DUMP" "$GEN" 120 > "$OUT/threaddump.log" 2>&1 &
  TD_PID=$!
fi
# S1_FAULT=1 이면 hold 시작 120초 뒤 60초간 장애를 걸고 차단기를 1초마다 표집한다 (tiny 는 10초 뒤 10초간)
# S1_FAULT_KIND: mock(기본) 모의 서버가 Claude·Voyage 세 경로를 500 으로 답함 / mock-embeddings Voyage 임베딩 경로만 500 / master-stop 채팅 클러스터 마스터 하나 docker stop -> start / cluster-pause 여섯 노드 docker pause -> unpause
FAULT_PID=""
if [ -n "${S1_FAULT:-}" ]; then
  python3 "$LOADTEST_DIR/collect_s1.py" fault --scenario "$SCENARIO" --out "$OUT" $TINY_FLAG --kind "${S1_FAULT_KIND:-mock}" > "$OUT/fault.log" 2>&1 &
  FAULT_PID=$!
fi
wait $GEN; GEN_RC=$?
wait $COL; COL_RC=$?
FAULT_RC=0
if [ -n "$FAULT_PID" ]; then wait $FAULT_PID; FAULT_RC=$?; fi
echo "[run] generator exit=$GEN_RC collector exit=$COL_RC fault exit=$FAULT_RC"
# 생성기·수집기·장애 표집기가 실패한 회차의 값은 측정이 아니다 - summary.json 을 만들지 않고 실패로 끝낸다
if [ "$GEN_RC" -ne 0 ] || [ "$COL_RC" -ne 0 ] || [ "$FAULT_RC" -ne 0 ]; then
  echo "[run] FAILED: generator exit=$GEN_RC collector exit=$COL_RC fault exit=$FAULT_RC (see $OUT/loadgen.log $OUT/collector.log $OUT/fault.log), no summary.json" >&2
  rm -f "$OUT/summary.json"
  exit 1
fi

# 생성기가 끝난 뒤에도 앱을 켜 둔 채 저장·요약 큐가 비는 시간을 재고 이번 회차의 chat_messages 행을 읽는다
rm -f "$OUT/drain.json"
python3 "$LOADTEST_DIR/collect_s1.py" drain --scenario "$SCENARIO" --out "$OUT" > "$OUT/drain.log" 2>&1 || { echo "[run] FAILED: drain watch (see $OUT/drain.log), no summary.json" >&2; exit 1; }
python3 "$LOADTEST_DIR/collect_s1.py" summarize --scenario "$SCENARIO" --out "$OUT" --code "$CODE" --commit "$COMMIT" --app-env "$S1_APP_ENV" || exit 1
grep -c "" "$OUT/app.log" | sed 's/^/[run] app.log lines: /'
# 다음 회차 전에 채팅 노드 여섯이 모두 돌고(일시정지 아님) 클러스터가 ok 인지 본다
bash "$S1_DIR/stack.sh" check || echo "[run] WARNING: chat cluster not healthy after the run" >&2
echo "[run] summary: $OUT/summary.json"
