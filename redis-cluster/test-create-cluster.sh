#!/bin/sh
# create-cluster.sh 가 이미 구성된 클러스터를 지우지 않는지 가짜 redis-cli 로 확인
# 배포마다 FLUSHALL + CLUSTER RESET HARD 가 돌면 진행 중인 채팅 세션이 전부 사라진다
# 사용: sh redis-cluster/test-create-cluster.sh (실제 Redis 불필요)

DIR=$(cd "$(dirname "$0")" && pwd)
WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

mkdir "$WORK/bin"

# 인자를 기록하고 상태 파일 값에 맞춰 cluster info 에 답한다, --cluster create 가 오면 상태가 ok 로 바뀐다
cat > "$WORK/bin/redis-cli" <<'STUB'
#!/bin/sh
echo "$*" >> "$FAKE_LOG"
case "$*" in
  *"--cluster create"*) echo ok > "$FAKE_STATE"; exit 0 ;;
  *ping*) echo PONG; exit 0 ;;
  *"cluster info"*)
    if [ "$(cat "$FAKE_STATE")" = ok ]; then
      printf 'cluster_state:ok\r\ncluster_slots_assigned:16384\r\n'
    else
      printf 'cluster_state:fail\r\ncluster_slots_assigned:0\r\n'
    fi
    exit 0 ;;
  *) exit 0 ;;
esac
STUB

# 스크립트의 대기 시간(sleep 3, sleep 2)을 없앤다
printf '#!/bin/sh\nexit 0\n' > "$WORK/bin/sleep"
chmod +x "$WORK/bin/redis-cli" "$WORK/bin/sleep"

FAILED=0

# 인자: 이름 초기상태 스크립트인자 FLUSHALL기대(yes|no)
run_case() {
  name=$1; state=$2; arg=$3; expect_flush=$4
  FAKE_LOG="$WORK/$name.log"; FAKE_STATE="$WORK/$name.state"
  : > "$FAKE_LOG"; echo "$state" > "$FAKE_STATE"
  export FAKE_LOG FAKE_STATE

  PATH="$WORK/bin:$PATH" sh "$DIR/create-cluster.sh" $arg > "$WORK/$name.out" 2>&1
  code=$?

  if grep -q FLUSHALL "$FAKE_LOG"; then got=yes; else got=no; fi
  reset=no; grep -q 'CLUSTER RESET' "$FAKE_LOG" && reset=yes

  if [ "$code" -eq 0 ] && [ "$got" = "$expect_flush" ] && [ "$reset" = "$expect_flush" ]; then
    echo "PASS $name (exit=$code flushall=$got reset=$reset)"
  else
    echo "FAIL $name (exit=$code flushall=$got reset=$reset, expected flush/reset=$expect_flush)"
    FAILED=1
  fi
}

run_case configured ok "" no
run_case fresh fail "" yes
run_case forced ok "--force" yes

exit $FAILED
