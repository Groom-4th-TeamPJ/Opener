-- V1: 초기 스키마 생성 (PostgreSQL)
-- Postgres: gen_random_uuid() 사용을 위한 확장 생성
-- CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Users 테이블
CREATE TABLE IF NOT EXISTS "users" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(500),
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1
);

-- Credentials 테이블
CREATE TABLE IF NOT EXISTS "credentials" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    provider TEXT,
    provider_id VARCHAR(500),  -- provider_Id → provider_id (일관성)
    email VARCHAR(500),
    password VARCHAR(500),
    last_login_at TIMESTAMP,  -- NOT NULL 제거 (첫 생성시는 null일 수 있음)
    failed_login_attempts BIGINT,
    last_failed_login_at TIMESTAMP,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,


    -- 외래키 제약조건
    CONSTRAINT fk_credentials_user FOREIGN KEY (user_id)
    REFERENCES "users"(id) ON DELETE CASCADE,

    -- 제약조건
    CONSTRAINT chk_credentials_provider CHECK (
        (provider IS NOT NULL AND provider_id IS NOT NULL) OR
        (email IS NOT NULL AND password IS NOT NULL)
    ),

    -- 중복 방지
    CONSTRAINT uk_credentials_provider UNIQUE (provider, provider_id),
    CONSTRAINT uk_credentials_email UNIQUE (email)
    );

CREATE INDEX IF NOT EXISTS idx_credentials_user_id ON "credentials"(user_id);
CREATE INDEX IF NOT EXISTS idx_credentials_email ON "credentials"(email);
CREATE INDEX IF NOT EXISTS idx_credentials_provider ON "credentials"(provider, provider_id);
CREATE INDEX IF NOT EXISTS idx_credentials_deleted_at ON "credentials"(deleted_at) WHERE deleted_at IS NULL;


-- Exams 테이블
CREATE TABLE IF NOT EXISTS "exams" (
    id BIGSERIAL PRIMARY KEY,
    exam_year INT NOT NULL,
    exam_type TEXT NOT NULL,
    name VARCHAR(500) NOT NULL,
    quantity BIGINT NOT NULL,
    time_limit BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,

    -- 제약조건
    CONSTRAINT chk_exam_year CHECK (exam_year >= 1900 AND exam_year <= 2100),
    CONSTRAINT chk_exam_quantity CHECK (quantity > 0),
    CONSTRAINT chk_exam_time_limit CHECK (time_limit > 0),

    -- 중복 방지 (같은 년도, 같은 유형의 문제 생성 금지)
    CONSTRAINT uk_exams_unique UNIQUE (exam_year, exam_type)
    );

CREATE INDEX IF NOT EXISTS idx_exams_year_type ON "exams"(exam_year, exam_type);
CREATE INDEX IF NOT EXISTS idx_exams_deleted_at ON "exams"(deleted_at) WHERE deleted_at IS NULL;

-- Questions 테이블
CREATE TABLE IF NOT EXISTS "questions" (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    passages JSONB NOT NULL,
    question_no BIGINT NOT NULL,
    options JSONB,  -- JSON → JSONB로 통일
    answer INT NOT NULL,
    category TEXT NOT NULL,
    point INT NOT NULL,
    question_type TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,

    -- 외래키 제약조건
    CONSTRAINT fk_questions_exam FOREIGN KEY (exam_id)
    REFERENCES "exams"(id) ON DELETE CASCADE,

    -- 제약조건
    CONSTRAINT chk_question_answer CHECK (answer > 0),
    CONSTRAINT chk_question_point CHECK (point > 0),
    CONSTRAINT chk_question_order CHECK (question_no > 0)
    );

CREATE INDEX IF NOT EXISTS idx_questions_exam_id ON "questions"(exam_id);
CREATE INDEX IF NOT EXISTS idx_questions_exam_order ON "questions"(exam_id, question_no);
CREATE INDEX IF NOT EXISTS idx_questions_category ON "questions"(category);
CREATE INDEX IF NOT EXISTS idx_questions_deleted_at ON "questions"(deleted_at) WHERE deleted_at IS NULL;

-- exam_Results 테이블
CREATE TABLE IF NOT EXISTS "exam_results" (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    exam_id BIGINT NOT NULL,
    total_score BIGINT NOT NULL,
    total_time_spent INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,

    -- 외래키 제약조건
    CONSTRAINT fk_exam_results_user FOREIGN KEY (user_id)
    REFERENCES "users"(id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_results_exam FOREIGN KEY (exam_id)
    REFERENCES "exams"(id) ON DELETE CASCADE,

    -- 제약조건
    CONSTRAINT chk_exam_results_time CHECK (total_time_spent >= 0)
);

CREATE INDEX IF NOT EXISTS idx_exam_results_exam_id ON "exam_results"(exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_results_user_id ON "exam_results"(user_id);
CREATE INDEX IF NOT EXISTS idx_exam_results_user_exam ON "exam_results"(user_id, exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_results_deleted_at ON "exam_results"(deleted_at) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS "question_results" (
    id BIGSERIAL PRIMARY KEY,
    exam_result_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected INT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false,
    time_spent BIGINT NOT NULL DEFAULT 0,
    is_opener BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,

    -- 외래키 제약조건
    CONSTRAINT fk_question_results_exam_result FOREIGN KEY (exam_result_id)
    REFERENCES "exam_results"(id) ON DELETE CASCADE,
    CONSTRAINT fk_question_results_question FOREIGN KEY (question_id)
    REFERENCES "questions"(id) ON DELETE CASCADE,

    -- 제약조건
    CONSTRAINT chk_question_results_time CHECK (time_spent >= 0),
    CONSTRAINT uk_exam_result_question UNIQUE (exam_result_id, question_id)
);

CREATE INDEX IF NOT EXISTS idx_question_results_question_id ON "question_results"(question_id);
CREATE INDEX IF NOT EXISTS idx_question_results_exam_result_id ON "question_results"(exam_result_id);
CREATE INDEX IF NOT EXISTS idx_question_results_correct ON "question_results"(is_correct);
CREATE INDEX IF NOT EXISTS idx_question_results_deleted_at ON "question_results"(deleted_at) WHERE deleted_at IS NULL;

-- Question_New 테이블
CREATE TABLE IF NOT EXISTS "question_new" (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    question_result_id BIGINT NOT NULL,
    passage TEXT NOT NULL,  -- VARCHAR(500) → TEXT (긴 지문 대비)
    options JSONB,
    answer BIGINT NOT NULL,
    category TEXT NOT NULL,  -- BIGINT → TEXT (카테고리는 텍스트가 더 적합)
    question_type TEXT NOT NULL,
    analysis TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,

    -- 외래키 제약조건
    CONSTRAINT fk_question_new_user FOREIGN KEY (user_id)
    REFERENCES "users"(id) ON DELETE CASCADE,
    CONSTRAINT fk_question_new_result FOREIGN KEY (question_result_id)
    REFERENCES "question_results"(id) ON DELETE CASCADE,

    -- 제약조건
    CONSTRAINT chk_question_new_answer CHECK (answer > 0)
    );

CREATE INDEX IF NOT EXISTS idx_question_new_question_result_id ON "question_new"(question_result_id);
CREATE INDEX IF NOT EXISTS idx_question_new_user_id ON "question_new"(user_id);
CREATE INDEX IF NOT EXISTS idx_question_new_category ON "question_new"(category);
CREATE INDEX IF NOT EXISTS idx_question_new_deleted_at ON "question_new"(deleted_at) WHERE deleted_at IS NULL;

-- user_cans 테이블
CREATE TABLE IF NOT EXISTS "user_cans" (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    current_cans INT NOT NULL DEFAULT 0,
    max_cans INT NOT NULL DEFAULT 100,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,

    -- 외래키 제약조건
    CONSTRAINT fk_user_cans_user FOREIGN KEY (user_id)
    REFERENCES "users"(id) ON DELETE CASCADE,

    -- 제약조건
    CONSTRAINT chk_user_cans_current CHECK (current_cans >= 0),
    CONSTRAINT chk_user_cans_max CHECK (max_cans >= 0),
    CONSTRAINT chk_user_cans_limit CHECK (current_cans <= max_cans),

    -- 중복 방지 (한 사용자당 하나의 캔 레코드)
    CONSTRAINT uk_user_cans_user UNIQUE (user_id)
    );

CREATE INDEX IF NOT EXISTS idx_user_cans_user_id ON "user_cans"(user_id);
CREATE INDEX IF NOT EXISTS idx_user_cans_deleted_at ON "user_cans"(deleted_at) WHERE deleted_at IS NULL;

-- can_usage_logs 테이블
CREATE TABLE IF NOT EXISTS "can_usage_logs" (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    usage_type TEXT NOT NULL,
    cans_used INT NOT NULL DEFAULT 1,
    related_id BIGINT,  -- NOT NULL 제거 (모든 로그가 related_id를 가지지 않을 수 있음)
    cans_before INT NOT NULL,
    cans_after INT NOT NULL,
    status VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,

    -- 외래키 제약조건
    CONSTRAINT fk_can_usage_logs_user FOREIGN KEY (user_id)
    REFERENCES "users"(id) ON DELETE CASCADE,

    -- 제약조건
    CONSTRAINT chk_can_usage_cans_used CHECK (cans_used > 0),
    CONSTRAINT chk_can_usage_cans_before CHECK (cans_before >= 0),
    CONSTRAINT chk_can_usage_cans_after CHECK (cans_after >= 0)
    );

CREATE INDEX IF NOT EXISTS idx_can_usage_logs_user_id ON "can_usage_logs"(user_id);
CREATE INDEX IF NOT EXISTS idx_can_usage_logs_type ON "can_usage_logs"(usage_type);
CREATE INDEX IF NOT EXISTS idx_can_usage_logs_created_at ON "can_usage_logs"(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_can_usage_logs_deleted_at ON "can_usage_logs"(deleted_at) WHERE deleted_at IS NULL;

-- chat_message 테이블
CREATE TABLE IF NOT EXISTS "chat_messages" (
    id BIGSERIAL PRIMARY KEY,
    question_result_id BIGINT NOT NULL,
    messages JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,

    -- 외래키 제약조건
    CONSTRAINT fk_chat_message_question_result FOREIGN KEY (question_result_id)
    REFERENCES "question_results"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chat_message_question_result_id ON "chat_messages"(question_result_id);
CREATE INDEX IF NOT EXISTS idx_chat_message_deleted_at ON "chat_messages"(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_chat_message_created_at_desc ON "chat_messages"(created_at DESC);

-- 코멘트 추가 (선택사항)
COMMENT ON TABLE "users" IS '사용자 정보';
COMMENT ON TABLE "exams" IS '시험 정보';
COMMENT ON TABLE "questions" IS '문제 정보';
COMMENT ON TABLE "exam_results" IS '사용자 시험 결과';
COMMENT ON TABLE "question_results" IS '문제별 풀이 기록';
COMMENT ON TABLE "question_new" IS 'AI가 생성한 신규 문제';
COMMENT ON TABLE "credentials" IS '인증 정보';
COMMENT ON TABLE "user_cans" IS '사용자 캔(포인트) 정보';
COMMENT ON TABLE "can_usage_logs" IS '캔 사용 로그';
COMMENT ON TABLE "chat_messages" IS 'LLM 채팅 메세지';