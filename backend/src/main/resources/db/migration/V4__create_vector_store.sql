-- pgvector extension 설치
CREATE EXTENSION IF NOT EXISTS vector;

-- Vector Store 테이블
CREATE TABLE vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    metadata JSONB,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- HNSW 인덱스 (빠른 코사인 유사도 검색)
CREATE INDEX vector_store_embedding_idx
ON vector_store
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- 메타데이터 GIN 인덱스
CREATE INDEX vector_store_metadata_idx
ON vector_store
USING gin (metadata jsonb_path_ops);

-- 생성 시간 인덱스
CREATE INDEX vector_store_created_at_idx
ON vector_store (created_at DESC);

-- Updated_at 자동 갱신
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_vector_store_updated_at
BEFORE UPDATE ON vector_store
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE vector_store IS 'Vector embeddings for RAG system';
COMMENT ON COLUMN vector_store.embedding IS 'OpenAI embedding (1536 dimensions)';
COMMENT ON COLUMN vector_store.metadata IS 'Document metadata (source, page, etc.)';
