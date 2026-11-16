-- 요약 없는 행만 색인한다 -> 요약 점검이 전체 행 훑기 없이 오래된 미요약 행을 찾는다, 요약이 채워지면 색인에서 빠진다
CREATE INDEX IF NOT EXISTS idx_chat_messages_summary_null
    ON chat_messages (created_at)
    WHERE summary IS NULL;
