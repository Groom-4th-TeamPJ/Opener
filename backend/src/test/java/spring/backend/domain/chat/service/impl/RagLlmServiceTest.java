package spring.backend.domain.chat.service.impl;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
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
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import reactor.test.StepVerifier;
import spring.backend.domain.chat.service.spec.ChatHistoryProvider;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;
import spring.backend.domain.chat.util.PromptLoader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 서킷은 구독 시점 오류만 센다 -> 검색·조립이 메서드 본문에서 동기로 던지면 집계 자체가 불가능하다
// 실패 주입 측정에서 이 경로가 실제로 드러났다(2026-08-08): 임베딩 I/O 오류가 500 으로 새고 세션이 갇혔다
@ExtendWith(MockitoExtension.class)
class RagLlmServiceTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private ChatHistoryProvider chatHistoryProvider;
    @Mock private VectorStore vectorStore;
    @Mock private PromptLoader promptLoader;

    private RagLlmService service;

    @BeforeEach
    void setUp() {
        // 실제 SimpleMeterRegistry 를 넣는다 -> 목이면 timer(...).record(Supplier) 가 null 을 돌려줘 검색 결과가 사라진다
        service = new RagLlmService(chatClientBuilder, chatHistoryProvider, vectorStore, promptLoader,
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
        when(chatClient.prompt().messages(sent.capture()).stream().chatResponse()).thenReturn(Flux.just(chunk("토큰", "")));

        StepVerifier.create(service.chatStream("1", "현재 질문", 7L)).expectNext("토큰").verifyComplete();

        assertEquals("현재 질문", search.getValue().getQuery());
        List<Message> messages = sent.getValue();
        assertEquals(List.of("시스템 규칙", "이전 질문", "이전 답", "현재 질문"),
                messages.stream().map(Message::getText).toList());
        assertInstanceOf(SystemMessage.class, messages.get(0));
        assertInstanceOf(UserMessage.class, messages.get(3));
        verify(chatHistoryProvider).getHistoryBefore("1", 7L);
    }

    // 거절 응답은 텍스트 없이 stop_reason=refusal 로 끝난다 -> 빈 답변이 정상 완료로 보이면 사용자는 빈 말풍선만 본다
    @Test
    @DisplayName("stop_reason 이 refusal 이면 이미 나온 토큰 뒤에 LLM 응답 실패 에러로 끝난다")
    void chatStream_refusal_LLM실패에러() {
        stubStream(Flux.just(chunk("일부", ""), chunk("", "refusal")));

        StepVerifier.create(service.chatStream("1", "질문", 7L))
                .expectNext("일부")
                .expectErrorSatisfies(e -> assertEquals(ErrorCode.LLM_RESPONSE_FAIL, ((BusinessException) e).getErrorCode()))
                .verify(TIMEOUT);
    }

    @Test
    @DisplayName("텍스트가 한 조각도 없이 끝나면 빈 완료 대신 LLM 응답 실패 에러다")
    void chatStream_텍스트없음_LLM실패에러() {
        stubStream(Flux.just(chunk("", "end_turn")));

        StepVerifier.create(service.chatStream("1", "질문", 7L))
                .expectErrorSatisfies(e -> assertEquals(ErrorCode.LLM_RESPONSE_FAIL, ((BusinessException) e).getErrorCode()))
                .verify(TIMEOUT);
    }

    @Test
    @DisplayName("stop_reason 이 대문자 REFUSAL 로 와도 거절로 본다")
    void chatStream_REFUSAL대문자_LLM실패에러() {
        stubStream(Flux.just(chunk("답", ""), chunk("", "REFUSAL")));

        StepVerifier.create(service.chatStream("1", "질문", 7L))
                .expectNext("답")
                .expectError(BusinessException.class)
                .verify(TIMEOUT);
    }

    @Test
    @DisplayName("첫 조각 뒤 20초 동안 아무것도 안 오면 타임아웃 에러로 끝난다")
    void chatStream_조각뒤20초무응답_타임아웃() {
        stubStream(Flux.just(chunk("첫", "")).concatWith(Flux.never()));

        StepVerifier.withVirtualTime(() -> service.chatStream("1", "질문", 7L))
                .expectNext("첫")
                .expectNoEvent(Duration.ofSeconds(19))
                .thenAwait(Duration.ofSeconds(1))
                .expectError(TimeoutException.class)
                .verify(TIMEOUT);
    }

    // 타임아웃은 BusinessException 이 아니므로 llm-chat 서킷이 실패로 센다 (전환 전과 같은 집계)
    @Test
    @DisplayName("멈춘 스트림의 타임아웃을 서킷이 실패로 집계한다")
    void chatStream_타임아웃_서킷이집계한다() {
        stubStream(Flux.never());
        CircuitBreaker breaker = CircuitBreaker.of("llm-chat-gap-test", CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(1)
                .build());

        StepVerifier.withVirtualTime(() -> service.chatStream("1", "질문", 7L)
                        .transformDeferred(CircuitBreakerOperator.of(breaker)))
                .thenAwait(Duration.ofSeconds(20))
                .expectError(TimeoutException.class)
                .verify(TIMEOUT);

        assertEquals(1, breaker.getMetrics().getNumberOfFailedCalls());
    }

    @Test
    @DisplayName("조각 사이가 19초씩 벌어져도 끝까지 완료된다")
    void chatStream_19초간격_완료() {
        stubStream(Flux.defer(() -> Flux.just(chunk("가", ""), chunk("", "end_turn"))
                .delayElements(Duration.ofSeconds(19))));

        StepVerifier.withVirtualTime(() -> service.chatStream("1", "질문", 7L))
                .thenAwait(Duration.ofSeconds(60))
                .expectNext("가")
                .expectComplete()
                .verify(TIMEOUT);
    }

    private void stubStream(Flux<ChatResponse> responses) {
        when(chatHistoryProvider.getHistoryBefore("1", 7L)).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
                .thenReturn(List.of(new Document("근거 문서")));
        when(promptLoader.buildChatRagSystemPromptWithRule("근거 문서")).thenReturn("시스템 규칙");
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt().messages(any(List.class)).stream().chatResponse()).thenReturn(responses);
    }

    private static ChatResponse chunk(String text, String finishReason) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(finishReason).build())));
    }
}
