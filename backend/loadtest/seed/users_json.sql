-- 생성기가 읽는 사용자·세션 목록 (reset 뒤 run.sh 가 뽑는다)
SELECT json_agg(x ORDER BY x->>'email') FROM (
  SELECT json_build_object(
    'email', c.email, 'password', 'r125-pass',
    'sessions', (SELECT json_agg(json_build_object('session_id', er.id, 'question_result_id', qr.id, 'question_id', qr.question_id) ORDER BY er.id)
                 FROM exam_results er JOIN question_results qr ON qr.exam_result_id = er.id WHERE er.user_id = c.user_id)
  ) AS x
  FROM credentials c WHERE c.email LIKE 'r125-user-%@r125.test'
) t;
