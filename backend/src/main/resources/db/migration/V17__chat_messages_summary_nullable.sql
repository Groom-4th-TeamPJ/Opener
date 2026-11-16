-- 요약을 저장 뒤에 따로 채우므로 그 사이 행은 summary 가 비어 있다
ALTER TABLE chat_messages ALTER COLUMN summary DROP NOT NULL;
