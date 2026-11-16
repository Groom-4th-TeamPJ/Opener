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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import spring.backend.domain.chat.service.spec.ChatHistoryProvider;
import spring.backend.domain.chat.util.PromptLoader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        // 실제 SimpleMeterRegistry 를 넣는다 -> 목이면 timer(...).record(Supplier) 가 null 을 돌려줘 검색 결과가 사라진다
        service = new OpenAiLlmService(chatClientBuilder, chatHistoryProvider, vectorStore, promptLoader,
                new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("벡터 검색이 실패해도 던지지 않고 에러 시그널로 내보낸다")
    void chatStream_벡터검색실패_예외대신에러시그널() {
        when(chatHistoryProvider.getHistoryBefore("1", 7L)).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
                .thenThrow(new IllegalStateException("임베딩 API 장애"));

        // 던지면 호출자의 doOnError 가 돌지 않아 세션이 PROCESSING 에 갇힌다
        var stream = assertDoesNotThrow(() -> service.chatStream("1", "질문", 7L));

        StepVerifier.create(stream)
                .expectError(IllegalStateException.class)
                .verify(TIMEOUT);
    }

    @Test
    @DisplayName("벡터 검색 실패를 서킷이 실패로 집계한다")
    void chatStream_벡터검색실패_서킷이집계한다() {
        when(chatHistoryProvider.getHistoryBefore("1", 7L)).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
                .thenThrow(new IllegalStateException("임베딩 API 장애"));

        CircuitBreaker breaker = CircuitBreaker.of("llm-chat-test", CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(1)
                .build());

        // 어스펙트가 하는 일과 같은 조립 - 조립 시점에 던지면 이 연산자는 실패를 볼 기회조차 없다
        StepVerifier.create(service.chatStream("1", "질문", 7L)
                        .transformDeferred(CircuitBreakerOperator.of(breaker)))
                .expectError(IllegalStateException.class)
                .verify(TIMEOUT);

        assertEquals(1, breaker.getMetrics().getNumberOfFailedCalls());
    }

    // 히스토리는 질문 직전까지만 오므로 현재 질문은 프롬프트 조립이 한 번만 붙인다
    // 검색어도 현재 질문이어야 한다 -> 히스토리의 마지막 사용자 메시지로 검색하면 이전 턴 근거가 실린다
    @Test
    @DisplayName("저장 길이로 읽은 히스토리 뒤에 현재 질문을 한 번 붙이고 검색어도 현재 질문으로 쓴다")
    void chatStream_히스토리와현재질문_시스템_히스토리_질문순서로보낸다() {
        when(chatHistoryProvider.getHistoryBefore("1", 7L)).thenReturn(List.of(
                new UserMessage("이전 질문"), new AssistantMessage("이전 답")));
        ArgumentCaptor<SearchRequest> search = ArgumentCaptor.forClass(SearchRequest.class);
        when(vectorStore.similaritySearch(search.capture())).thenReturn(List.of(new Document("근거 문서")));
        when(promptLoader.buildChatRagSystemPromptWithRule("근거 문서")).thenReturn("시스템 규칙");
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> sent = ArgumentCaptor.forClass(List.class);
        when(chatClient.prompt().messages(sent.capture()).stream().content()).thenReturn(Flux.just("토큰"));

        StepVerifier.create(service.chatStream("1", "현재 질문", 7L)).expectNext("토큰").verifyComplete();

        assertEquals("현재 질문", search.getValue().getQuery());
        List<Message> messages = sent.getValue();
        assertEquals(List.of("시스템 규칙", "이전 질문", "이전 답", "현재 질문"),
                messages.stream().map(Message::getText).toList());
        assertInstanceOf(SystemMessage.class, messages.get(0));
        assertInstanceOf(UserMessage.class, messages.get(3));
        verify(chatHistoryProvider).getHistoryBefore("1", 7L);
        verify(chatHistoryProvider, never()).getFullHistory("1");
    }
}
