#!/bin/bash
# 앱 JVM 스레드 덤프를 N 초마다 떠서 <출력 폴더>/td-<번호>.txt 로 쓴다 - 지켜볼 PID(생성기)가 살아 있는 동안 (r129 진단, R129-14)
# 사용: threaddump.sh <앱 pid 파일> <출력 폴더> <간격 초> <지켜볼 pid> [시작 지연 초]
# jcmd Thread.print 는 플랫폼 스레드만 보인다 (가상 스레드는 Thread.dump_to_file 이 필요하다, 여기서는 플랫폼 스레드와 그 스택 맨 위만 본다)
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
APP="$(cat "$1")"; OUT="$2"; EVERY="$3"; WATCH="$4"; DELAY="${5:-0}"
mkdir -p "$OUT"
sleep "$DELAY"
n=0
while kill -0 "$WATCH" 2>/dev/null; do
  if [ "${S1_THREAD_DUMP_KIND:-platform}" = "virtual" ]; then
    # 가상 스레드까지 포함한 텍스트 덤프 (JVM 이 파일을 직접 쓴다, 경로는 절대 경로)
    "$JAVA_HOME_21/bin/jcmd" "$APP" Thread.dump_to_file -format=text "$(cd "$OUT" && pwd)/tv-$(printf %03d $n).txt" > /dev/null 2>&1
  else
    "$JAVA_HOME_21/bin/jcmd" "$APP" Thread.print > "$OUT/td-$(printf %03d $n).txt" 2>&1
  fi
  n=$((n + 1))
  sleep "$EVERY"
done
