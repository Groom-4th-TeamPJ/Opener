#!/bin/sh
# S1 클러스터 초기화 - 첫 3개 노드가 마스터, 나머지 3개가 복제본 (포트는 CHAT_PORTS 환경 변수, 호스트 이름은 redis-chat-N)
set -e
sleep 2
set -- $CHAT_PORTS
i=1
NODES=""
for p in "$@"; do NODES="$NODES redis-chat-$i:$p"; i=$((i + 1)); done
info=$(redis-cli -h redis-chat-1 -p "$1" cluster info 2>/dev/null || true)
if echo "$info" | grep -q 'cluster_state:ok' && echo "$info" | grep -q 'cluster_slots_assigned:16384'; then
    echo "cluster already formed"
else
    i=1
    for p in "$@"; do
        redis-cli -h redis-chat-$i -p "$p" FLUSHALL > /dev/null 2>&1 || true
        redis-cli -h redis-chat-$i -p "$p" CLUSTER RESET HARD > /dev/null 2>&1 || true
        i=$((i + 1))
    done
    redis-cli --cluster create $NODES --cluster-replicas 1 --cluster-yes --cluster-timeout 5
fi
sleep 2
redis-cli -h redis-chat-1 -p "$1" cluster info | grep cluster_state
