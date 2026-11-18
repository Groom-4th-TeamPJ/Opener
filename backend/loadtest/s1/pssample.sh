#!/bin/bash
# 호스트 프로세스별 CPU 를 N 초마다 <출력 파일> 에 덧붙인다 - 지켜볼 PID 가 살아 있는 동안, CPU 상위 12개 (r129 진단: 호스트 유휴 0~1% 의 주인을 가린다)
# 사용: pssample.sh <출력 파일> <간격 초> <지켜볼 pid>
OUT="$1"; EVERY="$2"; WATCH="$3"
while kill -0 "$WATCH" 2>/dev/null; do
  { echo "## $(date -u +%H:%M:%S)"; ps -Ao pcpu,pid,comm -r | head -13 | sed 's#/Users/mskim/Library/Java/JavaVirtualMachines/##; s#/Contents/Home/bin/java#:java#'; } >> "$OUT"
  sleep "$EVERY"
done
