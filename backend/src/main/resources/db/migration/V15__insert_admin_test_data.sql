-- V15: Admin 테스트 계정 및 exam_id=1 시험 결과 데이터 삽입
-- 계정: admin@test.com / admin123!

-- 1. 사용자 생성
INSERT INTO users (id, name, role, created_at, version)
VALUES ('11111111-1111-1111-1111-111111111111'::uuid,
        'Admin Test User',
        'USER',
        CURRENT_TIMESTAMP,
        1);

-- 2. Credentials 생성 (FORM 로그인)
-- 패스워드: admin123! (BCrypt 해시)
INSERT INTO credentials (id, user_id, provider, email, password, created_at, version)
VALUES ('22222222-2222-2222-2222-222222222222'::uuid,
        '11111111-1111-1111-1111-111111111111'::uuid,
        'FORM',
        'test123@test.com',
        '$2a$10$HUqejvSdAcFuWD85nQA9z.j4sdUART90iqIAePu6uPt43A8rVm5j.',
        CURRENT_TIMESTAMP,
        1);

-- 3. User Cans 초기화
INSERT INTO user_cans (user_id, current_cans, max_cans, created_at, version)
VALUES ('11111111-1111-1111-1111-111111111111'::uuid,
        10,
        100,
        CURRENT_TIMESTAMP,
        1);

-- 4. Exam Result 생성 (exam_id = 1, 2023 수능)
-- 가정: 30문제 중 25개 정답, 5개 오답
INSERT INTO exam_results (user_id,
                          exam_id,
                          total_score,
                          total_time_spent,
                          correct_count,
                          incorrect_count,
                          opener_usage_count,
                          last_opener_usage_date,
                          created_at,
                          version)
VALUES ('11111111-1111-1111-1111-111111111111'::uuid,
        1,
        85, -- 총 점수
        3600, -- 총 소요 시간 (초) - 60분
        25, -- 정답 개수
        5, -- 오답 개수
        2, -- opener 사용 횟수
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        1);

-- 5. Question Results 생성 (exam_id = 1의 문제들에 대한 결과)
-- exam_result_id는 위에서 생성된 결과의 id를 참조
WITH exam_result AS (SELECT id
                     FROM exam_results
                     WHERE user_id = '11111111-1111-1111-1111-111111111111'::uuid
                       AND exam_id = 1
                     LIMIT 1),
     questions_for_exam AS (SELECT id, question_no, answer FROM questions WHERE exam_id = 1 ORDER BY question_no)
INSERT
INTO question_results (exam_result_id,
                       question_id,
                       selected,
                       is_correct,
                       time_spent,
                       is_opener,
                       opener_used_at,
                       created_at,
                       version)
SELECT (SELECT id FROM exam_result),
       q.id,
       CASE
           -- 처음 25개 문제: 정답 선택
           WHEN q.question_no <= 25 THEN q.answer
           -- 나머지 5개 문제: 오답 선택 (정답이 1이면 2, 아니면 1)
           ELSE CASE WHEN q.answer = 1 THEN 2 ELSE 1 END
           END                                                                  as selected,
       CASE WHEN q.question_no <= 25 THEN true ELSE false END                   as is_correct,
       120                                                                      as time_spent, -- 문제당 2분
       CASE WHEN q.question_no IN (26, 27) THEN true ELSE false END             as is_opener,  -- 26, 27번 문제에 opener 사용
       CASE WHEN q.question_no IN (26, 27) THEN CURRENT_TIMESTAMP ELSE NULL END as opener_used_at,
       CURRENT_TIMESTAMP,
       1
FROM questions_for_exam q;
