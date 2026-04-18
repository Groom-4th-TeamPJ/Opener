/**
 * 시나리오 E: 스레드 풀 고갈 후 복구 확인
 *
 * 목적: 200 VU 공격으로 스레드 풀을 완전히 고갈시킨 이후
 *       서버가 자동으로 정상 상태로 복구되는지 확인
 *
 * 두 시나리오 동시 실행:
 *   attack        — 200 VU, 30초 동안 고갈 유발
 *   recovery_check — 5 VU, 90초 동안 health 엔드포인트로 생존 여부 모니터링
 *
 * 관찰 포인트:
 *   - 30초 동안: recovery_check의 실패율 증가 (서버 과부하)
 *   - 30초 이후: recovery_check가 다시 100% 성공 → 자동 복구 확인
 *
 * 실행: k6 run k6/05_recovery.js
 */
import http from 'k6/http';
import {check, sleep} from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const HOLD_SECONDS = __ENV.HOLD_SECONDS || '8';

export const options = {
    scenarios: {
        // 고갈 유발 공격 (30초)
        attack: {
            executor: 'constant-vus',
            vus: 200,
            duration: '30s',
            exec: 'attackLoad',
        },
        // 서버 생존 모니터링 (공격 전 기간 + 복구 기간 포함, 90초)
        recovery_check: {
            executor: 'constant-vus',
            vus: 5,
            duration: '90s',
            startTime: '0s',
            exec: 'checkHealth',
        },
    },
};

// 공격: 스레드 점유 요청을 쉬지 않고 전송
export function attackLoad() {
    http.post(`${BASE_URL}/test/load?seconds=${HOLD_SECONDS}`);
    sleep(0.1);
}

// 모니터링: /actuator/health 로 서버 생존 확인
export function checkHealth() {
    const res = http.get(`${BASE_URL}/actuator/health`);
    check(res, {
        '서버 살아있음 (<500)': (r) => r.status < 500,
    });
    sleep(1);
}
