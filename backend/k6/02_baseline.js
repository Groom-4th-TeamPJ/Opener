/**
 * 시나리오 B: 정상 부하 기준선 (Baseline)
 *
 * 목적: 스레드 풀이 여유로운 상태(VU 10)의 응답 시간을 측정
 *       고갈 테스트 결과와 비교할 기준
 *
 * 예상: 스레드 풀 10개 내에서 처리 → p95 응답시간 수십 ms (즉시 반환)
 * 실행: k6 run k6/02_baseline.js
 */
import http from 'k6/http';
import {check, sleep} from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const HOLD_SECONDS = __ENV.HOLD_SECONDS || '8';

export const options = {
    vus: 10,        // core=10 이하 → 큐 진입 없음
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<500'],  // 비동기 즉시 반환 → 매우 빠름
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const res = http.post(`${BASE_URL}/test/load?seconds=${HOLD_SECONDS}`);

    check(res, {
        '200 OK': (r) => r.status === 200,
    });

    sleep(2); // 실제 사용자 행동 모방
}
