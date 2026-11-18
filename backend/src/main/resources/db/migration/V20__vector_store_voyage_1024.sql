-- 임베딩 모델을 OpenAI text-embedding-3-small(1536)에서 Voyage voyage-4(1024)로 바꾼다 (r129)
-- 차원이 다른 벡터는 같은 열에 섞을 수 없다 -> 기존 5,152행을 비우고 기동 시 색인이 새로 채운다
-- 이후 모델만 바꾸고 차원이 같으면 마이그레이션은 필요 없다 (app.voyage.model, app.voyage.output-dimension)
-- 열 타입을 바꾸려면 인덱스를 먼저 내려야 한다
DROP INDEX IF EXISTS vector_store_embedding_idx;
TRUNCATE TABLE vector_store;
ALTER TABLE vector_store ALTER COLUMN embedding TYPE vector(1024);
CREATE INDEX vector_store_embedding_idx
ON vector_store
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
-- V19 의 cosine_distance 함수 비용(100)은 차원과 무관하게 그대로 둔다
COMMENT ON COLUMN vector_store.embedding IS 'Voyage voyage-4 embedding (1024 dimensions)';
