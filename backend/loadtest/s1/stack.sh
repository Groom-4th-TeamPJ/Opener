#!/bin/bash
# 사용: stack.sh up|reset|down
#  up    - r125-s1 스택 기동, 클러스터 형성까지 대기
#  reset - 측정 DB 를 시드 템플릿에서 다시 만들고, 채팅 마스터 FLUSHALL, RabbitMQ 큐 비움
#  down  - 스택 정지 (-v 는 쓰지 않는다 - 시드 템플릿 DB 가 볼륨에 있다)
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
load_ports

wait_cluster() {
  for _ in $(seq 1 60); do
    docker exec r125s1-redis-chat-1 redis-cli -p "$CHAT_PORT_1" cluster info 2>/dev/null | grep -q 'cluster_state:ok' && \
    docker exec r125s1-redis-chat-1 redis-cli -p "$CHAT_PORT_1" cluster info | grep -q 'cluster_known_nodes:6' && return 0
    sleep 2
  done
  echo "cluster not ready" >&2; return 1
}

case "$1" in
  up)
    dc up -d --wait --wait-timeout 180 db redis-auth redis-chat-1 redis-chat-2 redis-chat-3 redis-chat-4 redis-chat-5 redis-chat-6 rabbitmq || exit 1
    dc up redis-cluster-init 2>&1 | tail -5
    wait_cluster
    ;;
  reset)
    psql_db -d postgres -c "DROP DATABASE IF EXISTS opener_s1 WITH (FORCE)" -c "CREATE DATABASE opener_s1 TEMPLATE opener_tpl" || exit 1
    # 마스터만 비운다 (복제본은 읽기 전용이라 FLUSHALL 이 거부되는 것이 정상, 마스터의 FLUSHALL 이 복제본에도 전파된다)
    for n in 1 2 3 4 5 6; do
      eval "p=\$CHAT_PORT_$n"
      c="r125s1-redis-chat-$n"
      role=$(docker exec "$c" redis-cli -p "$p" role | head -1)
      [ "$role" = "master" ] || continue
      out=$(docker exec "$c" redis-cli -p "$p" FLUSHALL 2>&1)
      [ "$out" = "OK" ] || { echo "reset FAILED: FLUSHALL on $c -> $out" >&2; exit 1; }
      size=$(docker exec "$c" redis-cli -p "$p" DBSIZE)
      [ "$size" = "0" ] || { echo "reset FAILED: $c DBSIZE=$size after FLUSHALL" >&2; exit 1; }
    done
    # 관리 API 의 messages 는 5초 간격으로 갱신돼 직후에 옛 값을 주므로 rabbitmqctl 로 정확한 수를 본다
    for q in chat.message.save.queue chat.message.save.dlq; do
      if docker exec r125s1-rabbitmq rabbitmqctl list_queues name --quiet 2>/dev/null | grep -qx "$q"; then
        docker exec r125s1-rabbitmq rabbitmqctl purge_queue "$q" >/dev/null 2>&1 || { echo "reset FAILED: purge_queue $q" >&2; exit 1; }
        left=$(docker exec r125s1-rabbitmq rabbitmqctl list_queues name messages --quiet 2>/dev/null | awk -v q="$q" '$1 == q {print $2}')
        [ "$left" = "0" ] || { echo "reset FAILED: queue $q has '$left' messages after purge" >&2; exit 1; }
      fi
    done
    echo "reset done (chat masters DBSIZE 0, queues 0)"
    ;;
  down)
    dc down
    ;;
  *) echo "usage: stack.sh up|reset|down" >&2; exit 2 ;;
esac
