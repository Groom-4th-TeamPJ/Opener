/**
 * 시나리오 D: 고정 RPS 한계 탐색
 *
 * 목적: "초당 N개 요청이 들어오면 서버가 버틸 수 있나?" 를 측정
 *       constant-arrival-rate executor는 VU 수가 아닌 RPS를 제어
 *
 * 계산 기준:
 *   스레드 8초 점유, 초당 20 요청 → 20 * 8 = 160개 동시 스레드 필요
 *   → 한계(150) 초과 → 고갈 발생 예상
 *
 *   초당 18 요청 이하 → 18 * 8 = 144 → 한계 이내 → 정상
 *
 * 실행: k6 run k6/04_rps_limit.js
 *        k6 run -e RPS=10 k6/04_rps_limit.js  # RPS 변경 실험
 */
import http from 'k6/http';
import {check} from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const HOLD_SECONDS = __ENV.HOLD_SECONDS || '8';
const RPS = parseInt(__ENV.RPS || '20');

export const options = {
    scenarios: {
        rps_test: {
            executor: 'constant-arrival-rate',
            rate: RPS,
            timeUnit: '1s',
            duration: '1m',
            preAllocatedVUs: 100,
            maxVUs: 200,
        },
    },
    thresholds: {
        http_req_duration: ['p(99)<3000'],
        http_req_failed: ['rate<0.01'],
    },
};

export default function () {
    const res = http.post(`${BASE_URL}/test/load?seconds=${HOLD_SECONDS}`);
    check(res, {'성공 (<500)': (r) => r.status < 500});
}
