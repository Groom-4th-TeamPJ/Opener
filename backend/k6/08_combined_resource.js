/**
 * 시나리오 G: DB + Redis 복합 부하 (실제 채팅 리소스 패턴 모사)
 *
 * 목적: 실제 채팅 저장 흐름(Redis 세션 조회 → DB 쿼리 → Redis 결과 저장)의
 *       리소스 경합을 LLM 비용 없이 재현하여, DB·Redis 풀이 동시에 받는 압력을 측정
 *
 * 원리: /test/combined-stress 는 ops 회 반복으로 Redis SET/GET → DB SELECT → Redis SET/DEL 수행
 *       응답 JSON {dbElapsed, redisElapsed, totalElapsed} 으로 리소스별 소요를 분리 반환
 *
 * 모니터링(같이 볼 것):
 *   - Grafana > Opener — HikariCP / JPA : pending / acquire p95
 *   - Grafana > Opener — HTTP API       : /test/combined-stress p95/p99
 *   - (신규) Chat Pipeline               : SSE 연결 수는 이 스크립트에선 0 (저장 경로만 측정)
 *
 * 실행: k6 run k6/08_combined_resource.js
 *        k6 run -e OPS=20 k6/08_combined_resource.js
 */
import http from 'k6/http';
import {check, sleep} from 'k6';
import {Trend} from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const OPS = __ENV.OPS || '5';

// 리소스별 소요를 서버 응답에서 분리 측정 → 어느 자원이 먼저 느려지는지 판별
const dbTrend = new Trend('combined_db_ms', true);
const redisTrend = new Trend('combined_redis_ms', true);
const totalTrend = new Trend('combined_total_ms', true);

export const options = {
    scenarios: {
        warmup: {executor: 'constant-vus', vus: 10, duration: '10s', exec: 'combined'},
        load: {executor: 'constant-vus', vus: 50, duration: '30s', startTime: '12s', exec: 'combined'},
        peak: {executor: 'constant-vus', vus: 100, duration: '15s', startTime: '44s', exec: 'combined'},
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        combined_total_ms: ['p(95)<1500'],
    },
};

export function combined() {
    const res = http.post(`${BASE_URL}/test/combined-stress?ops=${OPS}`);

    const ok = check(res, {
        '200 OK': (r) => r.status === 200,
    });

    if (ok) {
        try {
            const body = JSON.parse(res.body);
            if (body.dbElapsed != null) dbTrend.add(body.dbElapsed);
            if (body.redisElapsed != null) redisTrend.add(body.redisElapsed);
            if (body.totalElapsed != null) totalTrend.add(body.totalElapsed);
        } catch (e) {
            // 파싱 실패는 무시 — http_req_failed 로 이미 집계됨
        }
    }

    sleep(0.5);
}
