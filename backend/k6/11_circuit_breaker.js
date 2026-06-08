/**
 * 시나리오 J: 서킷브레이커 fast-fail — 이력서 M3 "외부 LLM 장애 시 p99 급감"
 *
 * 목적: 외부 호출 없이(/test/circuit 가상 장애) 서킷 동작을 재현
 *   1) CLOSED 구간: fail=true + delayMs(3s) 로 "느린 실패" → 응답 ~3s
 *   2) 실패율 50% 초과 → 서킷 OPEN
 *   3) OPEN 구간: 호출이 fallback 으로 즉시 차단 → fast-fail(~0ms)
 *   를 한 부하 안에서 관찰. http_req_duration 의 max(느린 실패) vs med/min(fast-fail) 대비.
 *
 * 관측: Grafana "Circuit Breaker State" 패널에서 test-circuit 이 OPEN 으로 전이하는 시점,
 *       같은 시간축에서 latency 가 3s → 즉시로 떨어지는 것을 함께 본다.
 *
 * 실행:
 *   docker compose -f docker-compose.test.yml --profile k6 run --rm k6 \
 *     run -o experimental-prometheus-rw --summary-export=/scripts/results/11_circuit.json \
 *     /scripts/11_circuit_breaker.js
 */
import http from 'k6/http';
import { Trend } from 'k6/metrics';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const DELAY_MS = __ENV.DELAY_MS || '3000'; // CLOSED 구간 느린 실패 지연

const fastfail = new Trend('cb_resp_ms'); // 응답시간 추세 (OPEN 후 급감 확인)

export const options = {
  scenarios: {
    circuit: {
      executor: 'constant-vus', vus: 10, duration: '70s',
    },
  },
  // OPEN 이후 fast-fail 이 지배적이 되므로 p95 는 낮게 수렴(서킷 효과). max 로 CLOSED 느린실패 확인
  thresholds: { 'http_req_failed': ['rate<0.05'] },
};

export default function () {
  const res = http.post(`${BASE_URL}/test/circuit?fail=true&delayMs=${DELAY_MS}`,
    null, { timeout: '30s' });
  fastfail.add(res.timings.duration);
  // fallback 도 200 으로 반환되므로 status 는 항상 200 (응답시간이 핵심 지표)
  check(res, { 'status 200': (r) => r.status === 200 });
}
