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
    deleted_at TIMESTAMP
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,

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
    "year" INT NOT NULL,
    exam_type TEXT NOT NULL,
    name VARCHAR(500) NOT NULL,
    quantity BIGINT NOT NULL,
    time_limit BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,

    -- 제약조건
    CONSTRAINT chk_exam_year CHECK ("year" >= 1900 AND "year" <= 2100),
    CONSTRAINT chk_exam_quantity CHECK (quantity > 0),
    CONSTRAINT chk_exam_time_limit CHECK (time_limit > 0),

    -- 중복 방지 (같은 년도, 같은 유형의 문제 생성 금지)
    CONSTRAINT uk_exams_unique UNIQUE ("year", exam_type)
    );

CREATE INDEX IF NOT EXISTS idx_exams_year_type ON "exams"("year", exam_type);
CREATE INDEX IF NOT EXISTS idx_exams_deleted_at ON "exams"(deleted_at) WHERE deleted_at IS NULL;

-- Questions 테이블
CREATE TABLE IF NOT EXISTS "questions" (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL,
    passages JSONB NOT NULL,
    "order" BIGINT NOT NULL,
    options JSONB,  -- JSON → JSONB로 통일
    answer INT NOT NULL,
    category TEXT NOT NULL,
    point INT NOT NULL,
    type TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,

    -- 외래키 제약조건
    CONSTRAINT fk_questions_exam FOREIGN KEY (exam_id)
    REFERENCES "exams"(id) ON DELETE CASCADE,

    -- 제약조건
    CONSTRAINT chk_question_answer CHECK (answer > 0),
    CONSTRAINT chk_question_point CHECK (point > 0),
    CONSTRAINT chk_question_order CHECK ("order" > 0)
    );

CREATE INDEX IF NOT EXISTS idx_questions_exam_id ON "questions"(exam_id);
CREATE INDEX IF NOT EXISTS idx_questions_exam_order ON "questions"(exam_id, "order");
CREATE INDEX IF NOT EXISTS idx_questions_category ON "questions"(category);
CREATE INDEX IF NOT EXISTS idx_questions_deleted_at ON "questions"(deleted_at) WHERE deleted_at IS NULL;

-- User_Results 테이블
CREATE TABLE IF NOT EXISTS "user_results" (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    exam_id BIGINT NOT NULL,
    total_score BIGINT NOT NULL,
    total_time_spent INT NOT NULL,
    attempt BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,

    -- 외래키 제약조건
    CONSTRAINT fk_user_results_user FOREIGN KEY (user_id)
    REFERENCES "users"(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_results_exam FOREIGN KEY (exam_id)
    REFERENCES "exams"(id) ON DELETE CASCADE,

    -- 제약조건
    CONSTRAINT chk_user_results_time CHECK (total_time_spent >= 0),
    CONSTRAINT chk_user_results_attempt CHECK (attempt > 0),

    -- 중복 방지 (같은 사용자가 같은 시험에 같은 시도 번호로 중복 제출 방지)
    CONSTRAINT uk_user_results_unique UNIQUE (user_id, exam_id, attempt)
);

CREATE INDEX IF NOT EXISTS idx_user_results_exam_id ON "user_results"(exam_id);
CREATE INDEX IF NOT EXISTS idx_user_results_user_id ON "user_results"(user_id);
CREATE INDEX IF NOT EXISTS idx_user_results_user_exam ON "user_results"(user_id, exam_id);
CREATE INDEX IF NOT EXISTS idx_user_results_deleted_at ON "user_results"(deleted_at) WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS "exam_history" (
    id BIGSERIAL PRIMARY KEY,
    user_result_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    "select" INT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT false,
    time_spent BIGINT NOT NULL DEFAULT 0,
    is_opener BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,

    -- 외래키 제약조건
    CONSTRAINT fk_exam_history_user_result FOREIGN KEY (user_result_id)
    REFERENCES "user_results"(id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_history_exam FOREIGN KEY (exam_id)
    REFERENCES "exams"(id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_history_question FOREIGN KEY (question_id)
    REFERENCES "questions"(id) ON DELETE CASCADE,

    -- 제약조건
    CONSTRAINT chk_exam_history_time CHECK (time_spent >= 0),

    -- 중복 방지 (같은 user_result에서 같은 문제를 중복 제출 방지)
    CONSTRAINT uk_exam_history_unique UNIQUE (user_result_id, question_id)
);
CREATE INDEX IF NOT EXISTS idx_exam_history_exam_id ON "exam_history"(exam_id);
CREATE INDEX IF NOT EXISTS idx_exam_history_question_id ON "exam_history"(question_id);
CREATE INDEX IF NOT EXISTS idx_exam_history_user_result_id ON "exam_history"(user_result_id);
CREATE INDEX IF NOT EXISTS idx_exam_history_correct ON "exam_history"(is_correct);
CREATE INDEX IF NOT EXISTS idx_exam_history_deleted_at ON "exam_history"(deleted_at) WHERE deleted_at IS NULL;

-- Question_New 테이블
CREATE TABLE IF NOT EXISTS "question_new" (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    history_id BIGINT NOT NULL,
    passage TEXT NOT NULL,  -- VARCHAR(500) → TEXT (긴 지문 대비)
    option JSONB NOT NULL,
    answer BIGINT NOT NULL,
    category TEXT NOT NULL,  -- BIGINT → TEXT (카테고리는 텍스트가 더 적합)
    type TEXT NOT NULL,
    analysis TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,

    -- 외래키 제약조건
    CONSTRAINT fk_question_new_user FOREIGN KEY (user_id)
    REFERENCES "users"(id) ON DELETE CASCADE,
    CONSTRAINT fk_question_new_history FOREIGN KEY (history_id)
    REFERENCES "exam_history"(id) ON DELETE CASCADE,

    -- 제약조건
    CONSTRAINT chk_question_new_answer CHECK (answer > 0)
    );

CREATE INDEX IF NOT EXISTS idx_question_new_history_id ON "question_new"(history_id);
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

-- 코멘트 추가 (선택사항)
COMMENT ON TABLE "users" IS '사용자 정보';
COMMENT ON TABLE "exams" IS '시험 정보';
COMMENT ON TABLE "questions" IS '문제 정보';
COMMENT ON TABLE "user_results" IS '사용자 시험 결과';
COMMENT ON TABLE "exam_history" IS '문제별 풀이 기록';
COMMENT ON TABLE "question_new" IS 'AI가 생성한 신규 문제';
COMMENT ON TABLE "credentials" IS '인증 정보';
COMMENT ON TABLE "user_cans" IS '사용자 캔(포인트) 정보';
COMMENT ON TABLE "can_usage_logs" IS '캔 사용 로그';