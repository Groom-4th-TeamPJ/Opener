/**
 * 시나리오 H: SSE 첫 토큰 지연(TTFT) 측정 — 이력서 M1-a "첫 토큰 ~300ms"
 *
 * 목적: 채팅 SSE 스트림에서 첫 토큰(첫 SSE 이벤트)까지 걸리는 시간을 측정
 *       기존 블로킹 구조(전체 응답 완료 대기)와 대비되는 "체감 응답" 지표
 *
 * 전제조건 (자동 순차 실행에서 제외한 이유):
 *   1) 실제 채팅 세션 id 필요 — connect 는 ?sessionId=<Long> 를 받는다
 *      (세션은 exam/opener 흐름에서 생성되므로 사전 준비 또는 ENV 주입 필요)
 *   2) 실제 OpenAI 호출 비용 발생 — mock LLM 또는 비용 통제 하에 실행 권장
 *   3) 표준 k6 http.get 은 SSE 스트림 전체를 받을 때까지 대기하므로
 *      첫 토큰 시점은 res.timings.waiting(TTFB) 으로 근사한다
 *      (정밀 측정은 xk6-sse 확장 사용)
 *
 * 실행:
 *   docker compose -f docker-compose.test.yml --profile k6 run --rm \
 *     -e SESSION_ID=123 k6 run /scripts/09_sse_ttft.js
 */
import http from 'k6/http';
import { Trend } from 'k6/metrics';
import { check, sleep } from 'k6';

const BASE_URL  = __ENV.BASE_URL || 'http://localhost:8080/api';
const EMAIL     = __ENV.TEST_EMAIL    || 'test@test.com';
const PASSWORD  = __ENV.TEST_PASSWORD || 'password123';
const SESSION_ID = __ENV.SESSION_ID;   // 필수: 실제 채팅 세션 id

const ttft = new Trend('sse_ttft_ms');   // 첫 토큰까지 시간(TTFB 근사)

export const options = {
  scenarios: {
    ttft: { executor: 'ramping-vus', startVUs: 0,
      stages: [{ duration: '20s', target: 20 }, { duration: '40s', target: 20 }] },
  },
  thresholds: {
    'sse_ttft_ms': ['p(95)<1000'],     // 첫 토큰 p95 1s 이내 목표
    'http_req_failed': ['rate<0.05'],
  },
};

function login() {
  const res = http.post(`${BASE_URL}/auth/form-login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } });
  const token = res.json('data.accessToken');
  return token;
}

export function setup() {
  if (!SESSION_ID) {
    throw new Error('SESSION_ID 환경변수가 필요합니다 (실제 채팅 세션 id)');
  }
  return { token: login() };
}

export default function (data) {
  const headers = { Authorization: `Bearer ${data.token}`, Accept: 'text/event-stream' };
  const start = Date.now();
  // connect: SSE 스트림. timings.waiting = 서버가 첫 바이트를 보내기까지 = 첫 토큰 근사
  const res = http.get(`${BASE_URL}/chat/connect?sessionId=${SESSION_ID}`,
    { headers, timeout: '60s' });
  ttft.add(res.timings.waiting);
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(1);
}
