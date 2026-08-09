-- 오프너 분석 경로 측정용 최소 시드
-- Question(지문·선택지) 1건과 그것을 가리키는 QuestionResult 1건이 있어야 /chat/analysis 가 끝까지 돈다
BEGIN;

INSERT INTO exams (id, exam_year, exam_type, name, quantity, time_limit, created_at, version)
VALUES (900001, 2026, 'MOCK', '측정용 모의고사', 1, 3600, now(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO questions (id, exam_id, passages, question_no, options, answer, category, point,
                       question_type, created_at, version)
VALUES (
  900001, 900001,
  '[{"order":1,"content":"이차함수 y = x^2 - 4x + 3 의 그래프가 x축과 만나는 두 점 사이의 거리를 구하시오. 그래프는 아래로 볼록하며 꼭짓점의 y좌표는 음수이다."}]'::jsonb,
  1,
  '[{"order":1,"content":"1"},{"order":2,"content":"2"},{"order":3,"content":"3"},{"order":4,"content":"4"},{"order":5,"content":"5"}]'::jsonb,
  2, '수학', 4, 'MULTIPLE_CHOICE', now(), 0
) ON CONFLICT (id) DO NOTHING;

INSERT INTO exam_results (id, user_id, exam_id, total_score, total_time_spent,
                          correct_count, incorrect_count, opener_usage_count, created_at, version)
SELECT 900001, u.id, 900001, 0, 0, 0, 1, 0, now(), 0
FROM users u WHERE u.name = '측정계정' ORDER BY u.created_at DESC LIMIT 1
ON CONFLICT (id) DO NOTHING;

INSERT INTO question_results (id, exam_result_id, question_id, selected, is_correct,
                              time_spent, is_opener, created_at, version)
VALUES (900001, 900001, 900001, 1, false, 30, true, now(), 0)
ON CONFLICT (id) DO NOTHING;

-- 오프너 1회당 can 1개 차감 -> 25회 측정에 넉넉하게 채움
UPDATE user_cans SET current_cans = 500, max_cans = 500;

COMMIT;
