#!/bin/bash
# S1 공통 함수 - stack.sh · run.sh · seed/build_template.sh 가 source 한다
# 모든 포트·JVM 옵션은 s1/scenario.json 한 곳에서 읽는다
S1_APP_ENV="${S1_APP_ENV:-}"
S1_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOADTEST_DIR="$(dirname "$S1_DIR")"
SCENARIO="$S1_DIR/scenario.json"
COMPOSE_FILE="$LOADTEST_DIR/compose/s1-stack.yml"
COMPOSE_PROJECT=r125-s1
JAVA_HOME_21=/Users/mskim/Library/Java/JavaVirtualMachines/temurin-21.0.13/Contents/Home
export PYTHONDONTWRITEBYTECODE=1

sc() { python3 -c "import json,sys; s=json.load(open('$SCENARIO')); print(eval(sys.argv[1]))" "$1"; }

load_ports() {
  PG_PORT=$(sc "s['ports']['postgres']"); AUTH_PORT=$(sc "s['ports']['redis_auth']")
  RABBIT_PORT=$(sc "s['ports']['rabbit']"); RABBIT_MGMT_PORT=$(sc "s['ports']['rabbit_mgmt']")
  APP_PORT=$(sc "s['ports']['app']"); MOCK_PORT=$(sc "s['ports']['mock']")
  for n in 1 2 3 4 5 6; do
    eval "CHAT_PORT_$n=$(sc "s['ports']['redis_chat'][$n-1]")"
    eval "CHAT_BUS_$n=$(sc "s['ports']['redis_chat_bus'][$n-1]")"
  done
  export PG_PORT AUTH_PORT RABBIT_PORT RABBIT_MGMT_PORT APP_PORT MOCK_PORT
  export CHAT_PORT_1 CHAT_PORT_2 CHAT_PORT_3 CHAT_PORT_4 CHAT_PORT_5 CHAT_PORT_6
  export CHAT_BUS_1 CHAT_BUS_2 CHAT_BUS_3 CHAT_BUS_4 CHAT_BUS_5 CHAT_BUS_6
  export COMPOSE_PROJECT_NAME=$COMPOSE_PROJECT
}

dc() { docker compose -p "$COMPOSE_PROJECT" -f "$COMPOSE_FILE" "$@"; }
psql_db() { docker exec -i r125s1-db psql -U dev -v ON_ERROR_STOP=1 "$@"; }

# 앱이 쓰는 환경 변수 (DB 이름만 인자)
export_app_env() {
  local db="$1"
  export SPRING_PROFILES_ACTIVE=dev
  export DB_URL="jdbc:postgresql://localhost:$PG_PORT/$db" DB_USERNAME=dev DB_PASSWORD=dev123
  export REDIS_HOST=localhost REDIS_PORT=$AUTH_PORT
  for n in 1 2 3 4 5 6; do eval "export REDIS_CLUSTER_NODE_$((n-1))=redis-chat-$n:\$CHAT_PORT_$n"; done
  export RABBITMQ_HOST=localhost RABBITMQ_PORT=$RABBIT_PORT RABBITMQ_USERNAME=guest RABBITMQ_PASSWORD=guest
  export OPENAI_API_KEY=sk-mock-r125
  export SPRING_AI_OPENAI_BASE_URL="http://localhost:$MOCK_PORT" SPRING_AI_OPENAI_CHAT_BASE_URL="http://localhost:$MOCK_PORT"
  export SERVER_PORT=$APP_PORT REST_API_KEY=r125-dummy CLIENT_SECRET_KEY=r125-dummy
}

port_free() { ! lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1; }

# 체크아웃의 bootJar 를 만들어 경로를 돌려준다 (bootRun 은 쓰지 않는다)
build_jar() {
  local checkout="$1"
  JAVA_HOME=$JAVA_HOME_21 "$checkout/backend/gradlew" -p "$checkout/backend" bootJar -x test -q --console=plain >&2 || return 1
  ls "$checkout"/backend/build/libs/*.jar | grep -v -- '-plain' | head -1
}

# start_app <jar> <db> <run dir>  - PID 를 <run dir>/app.pid 에 적는다 (작업 폴더는 .env 가 없는 run dir)
start_app() {
  local jar="$1" db="$2" dir="$3" opts
  port_free "$APP_PORT" || { echo "app port $APP_PORT busy" >&2; return 1; }
  mkdir -p "$dir"
  export_app_env "$db"
  opts=$(sc "' '.join(s['jvm_opts'])")
  ulimit -n 65536 2>/dev/null || ulimit -n "$(ulimit -Hn)"
  # 서브셸이 exec 로 java 로 바뀌어야 $! 가 java PID 가 된다
  # S1_APP_ENV="K=V K2=V2" - 시나리오 파일을 건드리지 않고 앱 환경 변수를 더한다 (A 측정의 리스너 동시성)
  (cd "$dir" && exec env $S1_APP_ENV "$JAVA_HOME_21/bin/java" $opts -jar "$jar") > "$dir/app.log" 2>&1 &
  echo $! > "$dir/app.pid"
}

wait_health() {
  local t=${1:-240} i=0
  until curl -sf "http://localhost:$APP_PORT/api/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; do
    sleep 2; i=$((i+2))
    if [ $i -ge "$t" ]; then echo "health timeout ${t}s" >&2; return 1; fi
  done
  return 0
}

# PID 파일의 프로세스만 종료
stop_pid() {
  local f="$1" pid
  [ -f "$f" ] || return 0
  pid=$(cat "$f")
  kill "$pid" 2>/dev/null
  for _ in $(seq 1 30); do kill -0 "$pid" 2>/dev/null || break; sleep 1; done
  kill -0 "$pid" 2>/dev/null && kill -9 "$pid" 2>/dev/null
  rm -f "$f"
}

start_mock() {
  local dir="$1"
  port_free "$MOCK_PORT" || { echo "mock port $MOCK_PORT busy" >&2; return 1; }
  mkdir -p "$dir"
  python3 "$LOADTEST_DIR/mock_openai.py" --scenario "$SCENARIO" > "$dir/mock.log" 2>&1 &
  echo $! > "$dir/mock.pid"
  for _ in $(seq 1 20); do curl -sf "http://localhost:$MOCK_PORT/stats" >/dev/null && return 0; sleep 0.5; done
  return 1
}
