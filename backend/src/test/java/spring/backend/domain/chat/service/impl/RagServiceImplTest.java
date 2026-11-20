package spring.backend.domain.chat.service.impl;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import spring.backend.domain.chat.util.PromptLoader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 오프너(유사 문제) 경로도 채팅 경로와 같은 규칙이다 (2026-11-20 R130-2)
// 벡터 검색(임베딩 API·DB) 실패를 오류로 내보내면 Claude 가 멀쩡해도 llm-rag 서킷이 실패를 세어 열린다
@ExtendWith(MockitoExtension.class)
class RagServiceImplTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    @Mock private VectorStore vectorStore;
    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private PromptLoader promptLoader;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private RagServiceImpl service;

    @BeforeEach
    void setUp() {
        // 실제 레지스트리 -> 목이면 timer(...).record(Supplier) 가 null 을 돌려줘 검색 결과가 사라진다
        service = new RagServiceImpl(vectorStore, chatClientBuilder, promptLoader, registry);
    }

    @Test
    @DisplayName("벡터검색실패: 에러 없이 RAG 없는 프롬프트로 답하고 검색은 한 번만 부른다")
    void generateSimilarProblemStream_벡터검색실패_RAG없는프롬프트로답한다() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new IllegalStateException("임베딩 API 장애"));
        when(promptLoader.buildNoRagOpenerAnalysisPrompt("문제")).thenReturn("일반 분석");
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        when(chatClient.prompt().user(sent.capture()).stream().chatResponse()).thenReturn(Flux.just(chunk("토큰")));

        StepVerifier.create(service.generateSimilarProblemStream("문제")).expectNext("토큰").verifyComplete();

        assertEquals("일반 분석", sent.getValue());
        // 실패한 의존성을 디버그 재검색으로 한 번 더 부르면 임베딩 호출이 두 배로 실패한다
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
        assertEquals(1.0, registry.counter("rag.vector.search.fallback", "path", "opener").count());
    }

    @Test
    @DisplayName("벡터검색실패: 서킷이 실패로 세지 않고 성공 1건으로 센다")
    void generateSimilarProblemStream_벡터검색실패_서킷이세지않는다() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new IllegalStateException("임베딩 API 장애"));
        when(promptLoader.buildNoRagOpenerAnalysisPrompt("문제")).thenReturn("일반 분석");
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt().user(any(String.class)).stream().chatResponse()).thenReturn(Flux.just(chunk("토큰")));

        CircuitBreaker breaker = CircuitBreaker.of("llm-rag-test", CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(1)
                .build());

        // 어스펙트가 하는 일과 같은 조립
        StepVerifier.create(service.generateSimilarProblemStream("문제")
                        .transformDeferred(CircuitBreakerOperator.of(breaker)))
                .expectNext("토큰")
                .expectComplete()
                .verify(TIMEOUT);

        assertEquals(0, breaker.getMetrics().getNumberOfFailedCalls());
        assertEquals(1, breaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    // 첫 검색이 0건으로 성공한 뒤 디버그 재검색만 실패해도 답은 이어진다 (2026-11-20 R130-7, 채팅 경로와 같은 규칙)
    @Test
    @DisplayName("디버그검색실패: 첫 검색이 0건이고 디버그 재검색이 던져도 RAG 없는 프롬프트로 답하고 서킷은 실패를 세지 않으며 폴백 카운터는 0이다")
    void generateSimilarProblemStream_디버그검색실패_답은_이어진다() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of())
                .thenThrow(new IllegalStateException("임베딩 API 장애"));
        when(promptLoader.buildNoRagOpenerAnalysisPrompt("문제")).thenReturn("일반 분석");
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        when(chatClient.prompt().user(sent.capture()).stream().chatResponse()).thenReturn(Flux.just(chunk("토큰")));

        CircuitBreaker breaker = CircuitBreaker.of("llm-rag-debug-test", CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(1)
                .build());

        StepVerifier.create(service.generateSimilarProblemStream("문제")
                        .transformDeferred(CircuitBreakerOperator.of(breaker)))
                .expectNext("토큰")
                .expectComplete()
                .verify(TIMEOUT);

        assertEquals("일반 분석", sent.getValue());
        verify(vectorStore, times(2)).similaritySearch(any(SearchRequest.class));
        assertEquals(0, breaker.getMetrics().getNumberOfFailedCalls());
        assertEquals(1, breaker.getMetrics().getNumberOfSuccessfulCalls());
        assertEquals(0.0, registry.counter("rag.vector.search.fallback", "path", "opener").count());
    }

    // 검색이 성공하고 0건인 경우는 전과 같다 -> 디버그 재검색이 이어진다
    @Test
    @DisplayName("검색이 빈 목록을 돌려주면 RAG 없는 프롬프트로 답하고 디버그 재검색을 하며 폴백 카운터는 오르지 않는다")
    void generateSimilarProblemStream_검색결과없음_디버그재검색() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(promptLoader.buildNoRagOpenerAnalysisPrompt("문제")).thenReturn("일반 분석");
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt().user(any(String.class)).stream().chatResponse()).thenReturn(Flux.just(chunk("토큰")));

        StepVerifier.create(service.generateSimilarProblemStream("문제")).expectNext("토큰").verifyComplete();

        verify(vectorStore, times(2)).similaritySearch(any(SearchRequest.class));
        assertEquals(0.0, registry.counter("rag.vector.search.fallback", "path", "opener").count());
    }

    @Test
    @DisplayName("검색이 문서를 돌려주면 RAG 프롬프트로 답한다")
    void generateSimilarProblemStream_검색성공_RAG프롬프트() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(new Document("근거")));
        when(promptLoader.buildRagOpenerAnalysisPrompt("근거", "문제")).thenReturn("근거 분석");
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        when(chatClient.prompt().user(sent.capture()).stream().chatResponse()).thenReturn(Flux.just(chunk("토큰")));

        StepVerifier.create(service.generateSimilarProblemStream("문제")).expectNext("토큰").verifyComplete();

        assertEquals("근거 분석", sent.getValue());
        assertEquals(0.0, registry.counter("rag.vector.search.fallback", "path", "opener").count());
    }

    private static ChatResponse chunk(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason("").build())));
    }
}
