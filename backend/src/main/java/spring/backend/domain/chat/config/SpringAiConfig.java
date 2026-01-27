package spring.backend.domain.chat.config;

import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 설정
 * Spring AI 2.0.0-M1부터는 자동 설정(Auto-configuration)을 통해
 * VectorStore와 EmbeddingModel 빈이 자동으로 생성됩니다.
 *
 * application.yml에서 다음 설정으로 제어:
 * - spring.ai.openai: OpenAI API 설정
 * - spring.ai.vectorstore.pgvector: PgVector 설정
 * - app.rag: RAG 기능 활성화 여부
 */
@Configuration
public class SpringAiConfig {

    /**
     * TokenTextSplitter 빈 생성
     * 문서를 작은 청크로 분할하여 벡터 임베딩을 효율적으로 생성
     * - defaultChunkSize: 청크 크기 (기본 800 토큰)
     * - minChunkSizeChars: 최소 청크 문자 수 (기본 350)
     * - minChunkLengthToEmbed: 임베딩할 최소 청크 길이 (기본 5)
     * - maxNumChunks: 최대 청크 개수 (기본 10000)
     * - keepSeparator: 구분자 유지 여부 (기본 true)
     */
    @Bean
    @ConditionalOnProperty(prefix = "app.rag", name = "enabled", havingValue = "true")
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }
}
