/**
 * 시나리오: Redis Connection Pool 부하 테스트
 *
 * 목적: Lettuce Connection Pool 적용 전/후 Redis 동시 접근 성능 비교.
 *       VU 50이 동시에 Redis SET/GET/DELETE를 반복하여 연결 풀 효과를 측정한다.
 *
 * 사용법:
 *   # Pool 적용 전 기준선 측정
 *   k6 run k6/06_redis_connection_pool.js
 *
 *   # VU 100으로 강한 부하
 *   k6 run --vus 100 k6/06_redis_connection_pool.js
 *
 *   # ops 파라미터 조정 (요청당 Redis 작업 횟수)
 *   k6 run -e OPS=50 k6/06_redis_connection_pool.js
 *
 * 실행: k6 run k6/06_redis_connection_pool.js
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const OPS      = __ENV.OPS      || '10';

// Custom metrics
const redisLatency = new Trend('redis_stress_latency', true);
const redisErrors  = new Counter('redis_stress_errors');

export const options = {
    scenarios: {
        // 1단계: 워밍업 (VU 10, 10초)
        warmup: {
            executor: 'constant-vus',
            vus: 10,
            duration: '10s',
            exec: 'redisStress',
        },
        // 2단계: 본 테스트 (VU 50, 30초)
        load: {
            executor: 'constant-vus',
            vus: 50,
            duration: '30s',
            startTime: '12s',
            exec: 'redisStress',
        },
        // 3단계: 피크 (VU 100, 15초)
        peak: {
            executor: 'constant-vus',
            vus: 100,
            duration: '15s',
            startTime: '44s',
            exec: 'redisStress',
        },
    },
    thresholds: {
        http_req_failed:          ['rate<0.05'],     // 실패율 5% 미만
        redis_stress_latency:     ['p95<500'],       // p95 500ms 미만
        http_req_duration:        ['p95<1000'],      // 전체 p95 1초 미만
    },
};

export function redisStress() {
    const res = http.post(`${BASE_URL}/test/redis-stress?ops=${OPS}`);

    const ok = check(res, {
        '200 OK': (r) => r.status === 200,
    });

    if (ok) {
        // 응답 body에서 소요시간(ms) 파싱
        const elapsed = parseInt(res.body, 10);
        if (!isNaN(elapsed)) {
            redisLatency.add(elapsed);
        }
    } else {
        redisErrors.add(1);
    }

    sleep(0.5);
}
