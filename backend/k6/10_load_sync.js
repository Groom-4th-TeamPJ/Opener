/**
 * 시나리오 I: 동기 점유 부하 — 스레드 종속성 A/B (이력서 M1-b "동시 20 → 200+")
 *
 * 목적: /test/load-sync 는 @Async 없이 요청 스레드가 직접 N초 슬립하므로
 *       연결당 스레드 점유가 실제로 발생한다. 이를 0→200 VU 로 가하여
 *         - VT off + Tomcat threads 20 (before): ~20 동시에서 포화 → 대기/지연 급증
 *         - VT on (after):                       200 동시도 스레드 종속 없이 수용
 *       의 차이를 측정한다.
 *
 * 실행:
 *   # after (VT on, 현재 서버)
 *   docker compose -f docker-compose.test.yml --profile k6 run --rm k6 \
 *     run -o experimental-prometheus-rw --summary-export=/scripts/results/10_after_vt_on.json \
 *     /scripts/10_load_sync.js
 *
 *   # before: 서버를 SPRING_THREADS_VIRTUAL_ENABLED=false, SERVER_TOMCAT_THREADS_MAX=20 로 재기동 후 동일 실행
 *   #   --summary-export=/scripts/results/10_before_vt_off.json
 */
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const HOLD = __ENV.SYNC_HOLD || '2'; // 요청 스레드 점유 시간(초)

export const options = {
  scenarios: {
    sync_load: {
      executor: 'ramping-vus', startVUs: 0,
      stages: [
        { duration: '20s', target: 50 },   // 정상
        { duration: '20s', target: 100 },  // 풀 초과 시작(before)
        { duration: '20s', target: 200 },  // 고갈 구간(before)
        { duration: '20s', target: 200 },  // 유지
        { duration: '10s', target: 0 },    // 복구
      ],
    },
  },
  thresholds: {
    'http_req_duration': ['p(95)<15000'],
    'http_req_failed': ['rate<0.1'],
  },
};

export default function () {
  // 동기 점유: 응답이 곧 점유 시간 → before(VT off)에서는 풀 대기로 지연 급증
  const res = http.post(`${BASE_URL}/test/load-sync?seconds=${HOLD}`, null, { timeout: '60s' });
  check(res, { 'status 200': (r) => r.status === 200 });
}
