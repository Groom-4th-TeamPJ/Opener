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
    for n in 1 2 3 4 5 6; do
      eval "p=\$CHAT_PORT_$n"
      docker exec "r125s1-redis-chat-$n" redis-cli -p "$p" FLUSHALL >/dev/null 2>&1   # 복제본은 읽기 전용이라 거부되는 것이 정상
    done
    for q in chat.message.save.queue chat.message.save.dlq; do
      curl -s -o /dev/null -u guest:guest -X DELETE "http://localhost:$RABBIT_MGMT_PORT/api/queues/%2F/$q/contents"
    done
    echo "reset done"
    ;;
  down)
    dc down
    ;;
  *) echo "usage: stack.sh up|reset|down" >&2; exit 2 ;;
esac
