#!/bin/sh
# Redis Cluster Initialization Script
# /redis-cluster/create-cluster.sh

set -e

echo "=========================================="
echo "Redis Cluster Initialization"
echo "=========================================="

echo "[1/4] Waiting for Redis nodes to be ready..."
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

echo "[2/4] Checking node connectivity..."
check_node redis-chat-1 6380
check_node redis-chat-2 6381
check_node redis-chat-3 6382
check_node redis-chat-4 6383
check_node redis-chat-5 6384
check_node redis-chat-6 6385

echo ""
echo "[3/4] Creating cluster..."
echo "  - 3 Masters: redis-chat-1:6380, redis-chat-2:6381, redis-chat-3:6382"
echo "  - 3 Replicas: redis-chat-4:6383, redis-chat-5:6384, redis-chat-6:6385"
echo ""

# Check if cluster already exists
CLUSTER_INFO=$(redis-cli -h redis-chat-1 -p 6380 cluster info 2>/dev/null || echo "cluster_state:none")

if echo "$CLUSTER_INFO" | grep -q "cluster_state:ok"; then
    echo "  - Cluster already exists and is healthy!"
    echo ""
    echo "[4/4] Current cluster status:"
    redis-cli -h redis-chat-1 -p 6380 cluster info | head -5
    echo ""
    redis-cli -h redis-chat-1 -p 6380 cluster nodes
    exit 0
fi

# Create the cluster
# --cluster-replicas 1 means each master gets 1 replica
# Note: Uses Docker hostnames for internal communication
# cluster-announce-ip in docker-compose handles external client access
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

echo ""
echo "[4/4] Verifying cluster status..."
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
