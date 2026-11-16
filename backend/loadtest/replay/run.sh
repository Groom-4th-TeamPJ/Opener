#!/bin/bash
# usage: run.sh up | step <before|after> <clients> <out.json> [warmup] [measure] | pass <before|after> <out.json> | down
# 결과 파일은 REPLAY_OUT 폴더(컨테이너 /out)에 쓴다 - 이름만 넘긴다
set -euo pipefail
cd "$(dirname "$0")"
export REPLAY_OUT="${REPLAY_OUT:-/Users/mskim/Desktop/PJ/Opener/.cascade/r125/measure/replay}"
mkdir -p "$REPLAY_OUT"
DC="docker compose -p r125-rp -f compose.yml"
RUN="$DC exec -T generator python3 /replay/turn_replay.py"

case "${1:-}" in
  up)
    $DC up -d
    $DC exec -T rp-node-1 redis-cli cluster info | grep -E 'cluster_state|cluster_slots_assigned'
    ;;
  step)
    # step <seq> <clients> <out> [warmup] [measure]
    $RUN step --seq "$2" --clients "$3" --out "/out/$4" --warmup "${5:-1}" --measure "${6:-3}" ${PROCS_MAX:+--procs-max "$PROCS_MAX"}
    ;;
  pass)
    # pass <seq> <out>
    $RUN pass --seq "$2" --out "/out/$3" ${PROCS_MAX:+--procs-max "$PROCS_MAX"}
    ;;
  down)
    # -v 는 쓰지 않는다 - 이 스택은 tmpfs 만 쓰므로 볼륨이 남지 않는다
    $DC down --remove-orphans
    ;;
  *)
    echo "usage: run.sh up | step <before|after> <clients> <out> [warmup] [measure] | pass <before|after> <out> | down" >&2
    exit 2
    ;;
esac
