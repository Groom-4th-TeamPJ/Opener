package spring.backend.domain.chat.service.impl;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.test.StepVerifier;
import spring.backend.domain.chat.service.spec.ChatHistoryProvider;
import spring.backend.domain.chat.util.PromptLoader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

// 서킷은 구독 시점 오류만 센다 -> 검색·조립이 메서드 본문에서 동기로 던지면 집계 자체가 불가능하다
// 실패 주입 측정에서 이 경로가 실제로 드러났다(2026-08-08): 임베딩 I/O 오류가 500 으로 새고 세션이 갇혔다
@ExtendWith(MockitoExtension.class)
class OpenAiLlmServiceTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private ChatHistoryProvider chatHistoryProvider;
    @Mock private VectorStore vectorStore;
    @Mock private PromptLoader promptLoader;

    private OpenAiLlmService service;

    @BeforeEach
    void setUp() {
        service = new OpenAiLlmService(chatClientBuilder, chatHistoryProvider, vectorStore, promptLoader);
        when(chatHistoryProvider.getRecentHistory("1")).thenReturn(List.of());
    }

    @Test
    @DisplayName("벡터 검색이 실패해도 던지지 않고 에러 시그널로 내보낸다")
    void chatStream_벡터검색실패_예외대신에러시그널() {
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
                .thenThrow(new IllegalStateException("임베딩 상류 장애"));

        // 던지면 호출자의 doOnError 가 돌지 않아 세션이 PROCESSING 에 갇힌다
        var stream = assertDoesNotThrow(() -> service.chatStream("1", "질문"));

        StepVerifier.create(stream)
                .expectError(IllegalStateException.class)
                .verify(TIMEOUT);
    }

    @Test
    @DisplayName("벡터 검색 실패를 서킷이 실패로 집계한다")
    void chatStream_벡터검색실패_서킷이집계한다() {
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
                .thenThrow(new IllegalStateException("임베딩 상류 장애"));

        CircuitBreaker breaker = CircuitBreaker.of("llm-chat-test", CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(1)
                .build());

        // 어스펙트가 하는 일과 같은 조립 - 조립 시점에 던지면 이 연산자는 실패를 볼 기회조차 없다
        StepVerifier.create(service.chatStream("1", "질문")
                        .transformDeferred(CircuitBreakerOperator.of(breaker)))
                .expectError(IllegalStateException.class)
                .verify(TIMEOUT);

        assertEquals(1, breaker.getMetrics().getNumberOfFailedCalls());
    }
}
