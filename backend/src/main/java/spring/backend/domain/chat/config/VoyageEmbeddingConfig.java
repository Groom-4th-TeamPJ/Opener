package spring.backend.domain.chat.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;
import spring.backend.domain.chat.embedding.VoyageEmbeddingModel;

// app.rag.enabled=true 일 때만 EmbeddingModel 을 만든다 -> VectorStoreConfig 와 같은 조건으로 짝을 맞춰 RAG 를 끄면 둘 다 없다
@Configuration
@ConditionalOnProperty(prefix = "app.rag", name = "enabled", havingValue = "true")
public class VoyageEmbeddingConfig {

    @Bean
    public EmbeddingModel voyageEmbeddingModel(
            @Value("${app.voyage.api-key:}") String apiKey,
            @Value("${app.voyage.base-url:https://api.voyageai.com}") String baseUrl,
            @Value("${app.voyage.model:voyage-4}") String model,
            @Value("${app.voyage.output-dimension:1024}") int outputDimension,
            // 0 이면 모델 상한의 80% -> 카드 없는 키가 분당 토큰 한도로 429 를 내면 이 값을 한도 이하로 낮춘다
            @Value("${app.voyage.max-request-tokens:0}") int maxRequestTokens,
            @Value("${app.voyage.max-attempts:6}") int maxAttempts) {
        Assert.hasText(apiKey, "app.voyage.api-key(VOYAGE_API_KEY) 가 비어 있음");
        int tokens = maxRequestTokens > 0 ? maxRequestTokens : VoyageEmbeddingModel.defaultMaxRequestTokens(model);
        return new VoyageEmbeddingModel(baseUrl, apiKey, model, outputDimension, tokens, maxAttempts, millis -> {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Voyage 재시도 대기 중 중단됨", e);
            }
        });
    }
}
