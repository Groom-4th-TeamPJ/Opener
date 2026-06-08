/**
 * 시나리오 F: DB 커넥션 풀(HikariCP) 포화 테스트
 *
 * 목적: VT 환경에서 스레드가 병목이 아니게 됐을 때의 "진짜 병목" 중 하나인
 *       HikariCP 커넥션 풀(max=20)의 포화 지점을 측정
 *
 * 원리: /test/db-stress 는 pg_sleep(N) 으로 커넥션을 N초 점유
 *       VU가 max=20 을 넘으면 커넥션 대기(pool exhaustion) 발생
 *       → hikaricp_connections_pending 상승, acquire latency 급증
 *
 * 모니터링(같이 볼 것): Grafana > Opener — HikariCP / JPA
 *   - hikaricp_connections_active 가 max(20)에 붙는가
 *   - hikaricp_connections_pending 가 0보다 커지는가  ← 포화의 결정적 신호
 *   - acquire p95 가 튀는가
 *
 * 실행: k6 run k6/07_db_connection_pool.js
 *        k6 run -e DB_HOLD=1 k6/07_db_connection_pool.js
 */
import http from 'k6/http';
import {check, sleep} from 'k6';
import {Trend} from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const DB_HOLD = __ENV.DB_HOLD || '2'; // 커넥션 점유 시간(초)

// 서버가 응답 body로 알려준 실제 DB 소요(ms)를 분리 측정
const dbLatency = new Trend('db_stress_latency', true);

export const options = {
    scenarios: {
        db_pool_saturation: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                {duration: '20s', target: 10},  // max(20) 이내 → 정상
                {duration: '20s', target: 20},  // max 도달 → 경계
                {duration: '30s', target: 40},  // max 초과 → pending 상승 예상
                {duration: '10s', target: 0},   // 복구 관찰
            ],
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        db_stress_latency: ['p(95)<8000'], // 점유 2s 기준, 대기 누적 시 초과
    },
};

export default function () {
    const res = http.post(`${BASE_URL}/test/db-stress?holdSeconds=${DB_HOLD}`);

    const ok = check(res, {
        '200 OK': (r) => r.status === 200,
    });

    if (ok) {
        // 응답 body = "1234ms" → 숫자만 파싱
        const elapsed = parseInt(res.body, 10);
        if (!isNaN(elapsed)) {
            dbLatency.add(elapsed);
        }
    }

    sleep(0.3);
}
