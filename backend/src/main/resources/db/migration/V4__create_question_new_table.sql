-- AI 생성 변형 문제 테이블
CREATE TABLE question_new (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    question_result_id BIGINT NOT NULL,
    passages JSONB NOT NULL,
    options JSONB,
    answer INTEGER,
    category VARCHAR(50) NOT NULL,
    question_type VARCHAR(50) NOT NULL,
    analysis TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_question_new_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_question_new_question_result FOREIGN KEY (question_result_id) REFERENCES question_result(id)
);

-- 인덱스
CREATE INDEX idx_question_new_user_id ON question_new(user_id);
CREATE INDEX idx_question_new_question_result_id ON question_new(question_result_id);
CREATE INDEX idx_question_new_category ON question_new(category);
CREATE INDEX idx_question_new_created_at ON question_new(created_at DESC);

-- 코멘트
COMMENT ON TABLE question_new IS 'RAG 기반으로 생성된 변형 문제';
COMMENT ON COLUMN question_new.passages IS '문제 지문 (JSONB)';
COMMENT ON COLUMN question_new.options IS '선택지 (JSONB)';
COMMENT ON COLUMN question_new.answer IS '정답 번호';
COMMENT ON COLUMN question_new.category IS '문제 카테고리 (ALG, GEO, PROB, CALC)';
COMMENT ON COLUMN question_new.question_type IS '문제 유형 (MCQ, FRQ)';
COMMENT ON COLUMN question_new.analysis IS '풀이 방법';
