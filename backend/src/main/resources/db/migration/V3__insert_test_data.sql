-- Mock 데이터 삽입: QuestionResult 테스트용
-- save-message API 테스트를 위한 샘플 데이터

-- 1. 테스트 사용자 (UUID 형식) - role은 USER 또는 ADMIN
INSERT INTO users (id, name, role, created_at, updated_at, version)
VALUES
    ('550e8400-e29b-41d4-a716-446655440000'::uuid, 'Test User', 'USER', NOW(), NOW(), 0);

-- 2. 테스트 자격증명 (Form 타입) - id를 UUID로 명시적으로 생성
INSERT INTO credentials (id, email, password, provider, user_id, created_at, updated_at, version)
VALUES
    ('650e8400-e29b-41d4-a716-446655440001'::uuid, 'test@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lLX.jvKYu1ueORxH2', 'FORM',
     '550e8400-e29b-41d4-a716-446655440000'::uuid, NOW(), NOW(), 0);

-- 3. 테스트 시험 (Exam) - exam_type은 M06, M09, CSAT 중 하나
INSERT INTO exams (exam_year, exam_type, name, quantity, time_limit, created_at, updated_at, version)
VALUES
    (2024, 'M06', '2024년 6월 모의고사', 5, 3600, NOW(), NOW(), 0);

-- 4. 테스트 문제 5개 (Question with JSONB) - category는 ALG, GEO, PROB, CALC 중 하나
INSERT INTO questions (question_no, category, point, question_type, passages, options, answer, exam_id, created_at, updated_at, version)
VALUES
    (1, 'ALG', 10, 'MCQ',
     '[{"type": "text", "content": "다음 방정식을 푸시오: x^2 - 5x + 6 = 0"}]'::jsonb,
     '[{"order": 1, "content": "x = 1, 6"}, {"order": 2, "content": "x = 2, 3"}, {"order": 3, "content": "x = -2, -3"}, {"order": 4, "content": "x = 1, -6"}]'::jsonb,
     2, (SELECT id FROM exams WHERE exam_type = 'M06' LIMIT 1), NOW(), NOW(), 0),

    (2, 'GEO', 10, 'MCQ',
     '[{"type": "text", "content": "삼각형의 넓이를 구하는 공식은?"}]'::jsonb,
     '[{"order": 1, "content": "밑변 x 높이"}, {"order": 2, "content": "밑변 x 높이 / 2"}, {"order": 3, "content": "2 x 밑변 x 높이"}, {"order": 4, "content": "밑변 + 높이"}]'::jsonb,
     2, (SELECT id FROM exams WHERE exam_type = 'M06' LIMIT 1), NOW(), NOW(), 0),

    (3, 'PROB', 10, 'MCQ',
     '[{"type": "text", "content": "주사위를 던질 때 짝수가 나올 확률은?"}]'::jsonb,
     '[{"order": 1, "content": "1/6"}, {"order": 2, "content": "1/3"}, {"order": 3, "content": "1/2"}, {"order": 4, "content": "2/3"}]'::jsonb,
     3, (SELECT id FROM exams WHERE exam_type = 'M06' LIMIT 1), NOW(), NOW(), 0),

    (4, 'CALC', 10, 'MCQ',
     '[{"type": "text", "content": "함수 f(x) = x^2의 도함수는?"}]'::jsonb,
     '[{"order": 1, "content": "x"}, {"order": 2, "content": "2x"}, {"order": 3, "content": "x^2"}, {"order": 4, "content": "2"}]'::jsonb,
     2, (SELECT id FROM exams WHERE exam_type = 'M06' LIMIT 1), NOW(), NOW(), 0),

    (5, 'ALG', 10, 'MCQ',
     '[{"type": "text", "content": "2x + 3 = 11일 때, x의 값은?"}]'::jsonb,
     '[{"order": 1, "content": "3"}, {"order": 2, "content": "4"}, {"order": 3, "content": "5"}, {"order": 4, "content": "6"}]'::jsonb,
     2, (SELECT id FROM exams WHERE exam_type = 'M06' LIMIT 1), NOW(), NOW(), 0);

-- 5. 테스트 시험 결과 (ExamResult)
INSERT INTO exam_results (user_id, exam_id, total_score, total_time_spent, created_at, updated_at, version)
VALUES
    ('550e8400-e29b-41d4-a716-446655440000'::uuid, (SELECT id FROM exams WHERE exam_type = 'M06' LIMIT 1), 40, 1800, NOW(), NOW(), 0);

-- 6. 테스트 문제 결과 5개 (QuestionResult) - BaseEntity를 상속하지 않으므로 created_at, updated_at, version 제거
INSERT INTO question_results (exam_result_id, question_id, selected, is_correct, time_spent, is_opener)
VALUES
    ((SELECT id FROM exam_results LIMIT 1), (SELECT id FROM questions WHERE question_no = 1 LIMIT 1), 2, true, 300, false),
    ((SELECT id FROM exam_results LIMIT 1), (SELECT id FROM questions WHERE question_no = 2 LIMIT 1), 2, true, 450, false),
    ((SELECT id FROM exam_results LIMIT 1), (SELECT id FROM questions WHERE question_no = 3 LIMIT 1), 3, true, 350, false),
    ((SELECT id FROM exam_results LIMIT 1), (SELECT id FROM questions WHERE question_no = 4 LIMIT 1), 2, true, 400, false),
    ((SELECT id FROM exam_results LIMIT 1), (SELECT id FROM questions WHERE question_no = 5 LIMIT 1), 2, true, 300, false);

-- 7. 테스트 사용자 CAN (캔 잔액)
INSERT INTO user_cans (user_id, current_cans, max_cans, created_at, updated_at, version)
VALUES
    ('550e8400-e29b-41d4-a716-446655440000'::uuid, 10, 10, NOW(), NOW(), 0);

-- 8. 테스트 CAN 사용 로그
INSERT INTO can_usage_logs (user_id, usage_type, cans_used, related_id, cans_before, cans_after, status, created_at, updated_at, version)
VALUES
    -- 초기 발급
    ('550e8400-e29b-41d4-a716-446655440000'::uuid, 'ISSUED', 10, NULL, 0, 10, 'SUCCESS', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', 0),
    -- 채팅 사용
    ('550e8400-e29b-41d4-a716-446655440000'::uuid, 'USED', 2, 1, 10, 8, 'SUCCESS', NOW() - INTERVAL '12 hours', NOW() - INTERVAL '12 hours', 0),
    -- 환불
    ('550e8400-e29b-41d4-a716-446655440000'::uuid, 'REFUNDED', 2, 1, 8, 10, 'SUCCESS', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours', 0);

-- 9. 테스트 변형 문제 (QuestionNew) - AI 생성 변형 문제 샘플 데이터
INSERT INTO question_new (user_id, question_result_id, passages, options, answer, category, question_type, analysis, created_at, updated_at, version)
VALUES
    -- 첫 번째 변형 문제 (question_no 1의 변형)
    ('550e8400-e29b-41d4-a716-446655440000'::uuid,
     (SELECT id FROM question_results WHERE question_id = (SELECT id FROM questions WHERE question_no = 1 LIMIT 1) LIMIT 1),
     '[{"order": 1, "type": "TEXT", "content": "다음 방정식을 푸시오: x^2 - 7x + 12 = 0"}]'::jsonb,
     '[{"order": 1, "content": "x = 2, 5"}, {"order": 2, "content": "x = 3, 4"}, {"order": 3, "content": "x = -3, -4"}, {"order": 4, "content": "x = 1, 12"}, {"order": 5, "content": "x = 2, 6"}]'::jsonb,
     2,
     'ALG',
     'MCQ',
     '## 풀이 방법

### 1단계: 인수분해
방정식 x^2 - 7x + 12 = 0을 인수분해합니다.
곱해서 12, 더해서 -7이 되는 두 수는 -3과 -4입니다.
(x - 3)(x - 4) = 0

### 2단계: 해 구하기
x - 3 = 0 또는 x - 4 = 0
따라서 x = 3 또는 x = 4

### 3단계: 최종 답
정답: x = 3, 4 (선택지 2번)',
     NOW(), NOW(), 0),

    -- 두 번째 변형 문제 (question_no 3의 변형 - 확률)
    ('550e8400-e29b-41d4-a716-446655440000'::uuid,
     (SELECT id FROM question_results WHERE question_id = (SELECT id FROM questions WHERE question_no = 3 LIMIT 1) LIMIT 1),
     '[{"order": 1, "type": "TEXT", "content": "주사위를 두 번 던질 때 합이 7이 나올 확률은?"}]'::jsonb,
     '[{"order": 1, "content": "1/12"}, {"order": 2, "content": "1/9"}, {"order": 3, "content": "1/6"}, {"order": 4, "content": "1/4"}, {"order": 5, "content": "1/3"}]'::jsonb,
     3,
     'PROB',
     'MCQ',
     '## 풀이 방법

### 1단계: 전체 경우의 수
주사위를 두 번 던지면 총 6 × 6 = 36가지 경우가 있습니다.

### 2단계: 합이 7이 되는 경우
(1,6), (2,5), (3,4), (4,3), (5,2), (6,1) → 총 6가지

### 3단계: 확률 계산
P(합이 7) = 6/36 = 1/6

### 4단계: 최종 답
정답: 1/6 (선택지 3번)',
     NOW(), NOW(), 0);
