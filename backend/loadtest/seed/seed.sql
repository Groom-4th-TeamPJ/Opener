-- S1 시드 - Flyway 적용 뒤의 템플릿 DB 에 사용자 1,000명 · 세션 10,000개를 넣는다
-- 사용자마다: 폼 로그인 자격(credentials, BCrypt) · Can 잔액 · 시험 결과 10개(채팅 세션으로 사용) · 시험 결과마다 문항 결과 1개(저장 대상)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 비밀번호는 전원 같은 값, 해시는 한 번만 계산
CREATE TEMP TABLE pw AS SELECT crypt('r125-pass', gen_salt('bf', 10)) AS h;

WITH u AS (
    INSERT INTO users (name, role)
    SELECT 'r125-user-' || lpad(i::text, 4, '0'), 'USER' FROM generate_series(1, 1000) i
    RETURNING id, name
), c AS (
    INSERT INTO credentials (user_id, provider, email, password, failed_login_attempts)
    SELECT u.id, 'FORM', u.name || '@r125.test', (SELECT h FROM pw), 0 FROM u
    RETURNING user_id
)
INSERT INTO user_cans (user_id, current_cans, max_cans)
SELECT user_id, 1000000, 1000000 FROM c;

-- 시험 결과 10개씩 (시험 1~12 에 돌려 배정)
INSERT INTO exam_results (user_id, exam_id)
SELECT u.id, 1 + (g % 12)
FROM users u CROSS JOIN generate_series(1, 10) g
WHERE u.name LIKE 'r125-user-%';

-- 시험 결과마다 그 시험의 첫 문항에 대한 문항 결과 1개
INSERT INTO question_results (exam_result_id, question_id, selected)
SELECT er.id, (SELECT min(q.id) FROM questions q WHERE q.exam_id = er.exam_id), 1
FROM exam_results er JOIN users u ON u.id = er.user_id
WHERE u.name LIKE 'r125-user-%';

ANALYZE;
