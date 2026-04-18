/**
 * 시나리오 C: 스레드 풀 고갈 테스트
 *
 * 목적: VU를 단계적으로 늘려 스레드 풀 고갈 시점(151번째 요청)을 확인
 *
 * 스레드 풀 설정 (AsyncConfig.java):
 *   corePoolSize  = 10
 *   maxPoolSize   = 50
 *   queueCapacity = 100
 *   → 동시 처리 한계 = 50 + 100 = 150
 *   → 151번째 요청 → RejectedExecutionException → HTTP 500
 *
 * 관찰 포인트:
 *   - VU ~50  : 정상 처리 (core 스레드 범위)
 *   - VU ~150 : 큐 대기 시작 (스레드 반환 후 순차 처리)
 *   - VU ~151+: http_req_failed 급등, HTTP 500 발생
 *
 * 실행: k6 run k6/03_thread_exhaustion.js
 */
import http from 'k6/http';
import {check, sleep} from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const HOLD_SECONDS = __ENV.HOLD_SECONDS || '8';

export const options = {
    scenarios: {
        thread_pool_exhaustion: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                {duration: '20s', target: 50},  // 0 → 50  (정상 범위)
                {duration: '20s', target: 100},  // 50 → 100 (큐 진입 시작)
                {duration: '20s', target: 160},  // 100 → 160 (고갈 예상 구간)
                {duration: '20s', target: 200},  // 160 → 200 (완전 고갈)
                {duration: '20s', target: 0},  // 복구 관찰
            ],
        },
    },
    thresholds: {
        // 이 임계값들이 깨지는 시점 = 스레드 풀 한계
        http_req_duration: ['p95<5000'],
        http_req_failed: ['rate<0.05'],
    },
};

export default function () {
    const res = http.post(`${BASE_URL}/test/load?seconds=${HOLD_SECONDS}`);

    const ok = check(res, {
        '200 OK': (r) => r.status === 200,
        '500 아님': (r) => r.status !== 500,
    });

    if (!ok) {
        console.log(`[실패] VU=${__VU} ITER=${__ITER} status=${res.status} body=${res.body.slice(0, 100)}`);
    }

    sleep(0.5); // 최소 대기 — 스레드 지속 점유 유도
}
