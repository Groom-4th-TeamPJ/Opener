package spring.backend.domain.chat.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

// @ConditionalOnProperty -> app.rag.enabled=true 일 때만 빈 등록, RAG 끄면 pgvector 의존성 없이도 부팅
@Configuration
@ConditionalOnProperty(
        prefix = "app.rag",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class VectorStoreConfig {

    @Bean
    public VectorStore pgVectorStore(
            JdbcTemplate jdbcTemplate,
            EmbeddingModel embeddingModel
    ) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .schemaName("public")
                .vectorTableName("vector_store")
                // 1536 = text-embedding-3-small 출력 차원 -> 임베딩 모델과 반드시 일치해야 검색 동작
                .dimensions(1536)
                // 코사인 거리 -> 문장 의미 유사도에 적합, 벡터 길이 영향 제거
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                // HNSW 인덱스 -> 대량 벡터에서도 근사 최근접 탐색을 빠르게, 풀스캔 방지
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .removeExistingVectorStoreTable(false)        // 기존 데이터 보존 -> 재기동 시 임베딩 유실 방지
                .initializeSchema(false)                      // 스키마는 Flyway 단일 관리 -> 이중 생성 충돌 방지
                .build();
    }
}
