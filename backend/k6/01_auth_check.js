/**
 * 시나리오 A: 인증 동작 확인
 *
 * 목적: k6 → 백엔드 연결 및 로그인이 정상 동작하는지 1회 확인.
 * 실행: k6 run k6/01_auth_check.js
 *
 * 주의: 로그인 URL은 /api/auth/form-login (context-path /api 포함)
 */
import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api';
const EMAIL    = __ENV.TEST_EMAIL    || 'test@test.com';
const PASSWORD = __ENV.TEST_PASSWORD || 'password123';

export const options = {
    vus: 1,
    iterations: 1,
};

export default function () {
    const loginRes = http.post(
        `${BASE_URL}/auth/form-login`,
        JSON.stringify({ email: EMAIL, password: PASSWORD }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    check(loginRes, {
        '로그인 성공 (200)': (r) => r.status === 200,
    });

    console.log('status :', loginRes.status);
    console.log('body   :', loginRes.body.slice(0, 200));

    // 슬로우 엔드포인트 접근 확인 (인증 없이 가능)
    const testRes = http.post(`${BASE_URL}/test/load?seconds=2`);
    check(testRes, {
        '/test/load 접근 가능 (200)': (r) => r.status === 200,
    });
    console.log('/test/load status:', testRes.status);
}
