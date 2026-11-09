#!/bin/sh
# Redis Cluster Initialization Script
# /redis-cluster/create-cluster.sh

set -e

# --force: 이미 구성된 클러스터도 의도적으로 지우고 새로 만든다 (세션 데이터 전부 삭제)
FORCE=0
if [ "$1" = "--force" ]; then
    FORCE=1
fi

echo "=========================================="
echo "Redis Cluster Initialization"
echo "=========================================="

echo "[1/5] Waiting for Redis nodes to be ready..."
sleep 3

# Check each node is ready
check_node() {
    local host=$1
    local port=$2
    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if redis-cli -h $host -p $port ping > /dev/null 2>&1; then
            echo "  - $host:$port is ready"
            return 0
        fi
        echo "  - Waiting for $host:$port (attempt $attempt/$max_attempts)..."
        sleep 1
        attempt=$((attempt + 1))
    done

    echo "  - ERROR: $host:$port is not responding"
    return 1
}

echo "[2/5] Checking node connectivity..."
check_node redis-chat-1 6380
check_node redis-chat-2 6381
check_node redis-chat-3 6382
check_node redis-chat-4 6383
check_node redis-chat-5 6384
check_node redis-chat-6 6385

# 어느 노드든 cluster_state:ok 이고 슬롯 16384 개가 배정돼 있으면 이미 구성된 클러스터
# 배포마다 지우고 다시 만들면 진행 중인 채팅 세션이 전부 사라진다
cluster_already_formed() {
    for node_info in "redis-chat-1 6380" "redis-chat-2 6381" "redis-chat-3 6382" \
                     "redis-chat-4 6383" "redis-chat-5 6384" "redis-chat-6 6385"; do
        host=$(echo $node_info | cut -d' ' -f1)
        port=$(echo $node_info | cut -d' ' -f2)
        info=$(redis-cli -h $host -p $port cluster info 2>/dev/null) || continue
        if echo "$info" | grep -q 'cluster_state:ok' && echo "$info" | grep -q 'cluster_slots_assigned:16384'; then
            return 0
        fi
    done
    return 1
}

if [ "$FORCE" -eq 0 ] && cluster_already_formed; then
    echo ""
    echo "[3/5] Cluster already formed (cluster_state:ok, 16384 slots) - skipping reset and create"
    echo "[4/5] Use --force to wipe and recreate"
else
    echo ""
    echo "[3/5] Resetting all nodes..."
    # 새로 만들거나 깨진 클러스터는 Docker 재시작으로 컨테이너 IP가 바뀌어 nodes.conf 의 IP가 stale 이므로
    # 리셋 후 현재 IP로 재구성한다
    for node_info in "redis-chat-1 6380" "redis-chat-2 6381" "redis-chat-3 6382" \
                     "redis-chat-4 6383" "redis-chat-5 6384" "redis-chat-6 6385"; do
        host=$(echo $node_info | cut -d' ' -f1)
        port=$(echo $node_info | cut -d' ' -f2)
        # 복제본 노드는 FLUSHALL 이 거부될 수 있어 실패해도 이어서 리셋한다
        redis-cli -h $host -p $port FLUSHALL > /dev/null 2>&1 \
            || echo "  - $host:$port FLUSHALL rejected"
        redis-cli -h $host -p $port CLUSTER RESET HARD > /dev/null 2>&1 \
            || echo "  - $host:$port CLUSTER RESET rejected"
        echo "  - $host:$port reset"
    done

    echo ""
    echo "[4/5] Creating cluster..."
    echo "  - 3 Masters: redis-chat-1:6380, redis-chat-2:6381, redis-chat-3:6382"
    echo "  - 3 Replicas: redis-chat-4:6383, redis-chat-5:6384, redis-chat-6:6385"
    echo ""

    redis-cli --cluster create \
        redis-chat-1:6380 \
        redis-chat-2:6381 \
        redis-chat-3:6382 \
        redis-chat-4:6383 \
        redis-chat-5:6384 \
        redis-chat-6:6385 \
        --cluster-replicas 1 \
        --cluster-yes \
        --cluster-timeout 5
fi

echo ""
echo "[5/5] Verifying cluster status..."
sleep 2

# Verify cluster state
FINAL_STATE=$(redis-cli -h redis-chat-1 -p 6380 cluster info | grep cluster_state | cut -d: -f2 | tr -d '[:space:]')

if [ "$FINAL_STATE" = "ok" ]; then
    echo ""
    echo "=========================================="
    echo "Cluster created successfully!"
    echo "=========================================="
    echo ""
    echo "Cluster Info:"
    redis-cli -h redis-chat-1 -p 6380 cluster info | head -10
    echo ""
    echo "Node List:"
    redis-cli -h redis-chat-1 -p 6380 cluster nodes
    echo ""
    echo "Slot Distribution:"
    redis-cli -h redis-chat-1 -p 6380 cluster slots
else
    echo ""
    echo "=========================================="
    echo "ERROR: Cluster creation failed!"
    echo "=========================================="
    echo "State: $FINAL_STATE"
    exit 1
fi
