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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 서킷은 구독 시점 오류만 센다 -> 히스토리 조회 같은 본문 동기 예외는 defer 안에 둬야 집계된다
// 벡터 검색(임베딩 API·DB) 실패는 이와 별개다 -> 오류로 내보내면 llm-chat 이 Claude 가 멀쩡한데도 열려 전원이 실패한다
// 그래서 검색 실패는 근거 없음으로 보고 규칙 프롬프트만으로 답한다 (2026-11-20 R130-2)
@ExtendWith(MockitoExtension.class)
class RagLlmServiceTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private ChatHistoryProvider chatHistoryProvider;
    @Mock private VectorStore vectorStore;
    @Mock private PromptLoader promptLoader;

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private RagLlmService service;

    @BeforeEach
    void setUp() {
        // 실제 SimpleMeterRegistry 를 넣는다 -> 목이면 timer(...).record(Supplier) 가 null 을 돌려줘 검색 결과가 사라진다
        service = new RagLlmService(chatClientBuilder, chatHistoryProvider, vectorStore, promptLoader,
                registry);
    }

    @Test
    @DisplayName("벡터검색실패: 에러 없이 규칙 프롬프트만으로 답하고 검색은 한 번만 부른다")
    void chatStream_벡터검색실패_규칙프롬프트로답한다() {
        when(chatHistoryProvider.getHistoryBefore("1", 7L)).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new IllegalStateException("임베딩 API 장애"));
        when(promptLoader.loadChatRulePrompt()).thenReturn("규칙만");
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> sent = ArgumentCaptor.forClass(List.class);
        when(chatClient.prompt().messages(sent.capture()).stream().chatResponse()).thenReturn(Flux.just(chunk("토큰", "")));

        StepVerifier.create(service.chatStream("1", "질문", 7L)).expectNext("토큰").verifyComplete();

        assertEquals(List.of("규칙만", "질문"), sent.getValue().stream().map(Message::getText).toList());
        // 실패한 의존성을 디버그 재검색으로 한 번 더 부르면 임베딩 호출이 두 배로 실패한다
        verify(vectorStore, times(1)).similaritySearch(any(SearchRequest.class));
        assertEquals(1.0, registry.counter("rag.vector.search.fallback", "path", "chat").count());
    }

    @Test
    @DisplayName("벡터검색실패: 서킷이 실패로 세지 않고 성공 1건으로 센다")
    void chatStream_벡터검색실패_서킷이세지않는다() {
        when(chatHistoryProvider.getHistoryBefore("1", 7L)).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new IllegalStateException("임베딩 API 장애"));
        when(promptLoader.loadChatRulePrompt()).thenReturn("규칙만");
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt().messages(any(List.class)).stream().chatResponse()).thenReturn(Flux.just(chunk("토큰", "")));

        CircuitBreaker breaker = CircuitBreaker.of("llm-chat-test", CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(1)
                .build());

        // 어스펙트가 하는 일과 같은 조립
        StepVerifier.create(service.chatStream("1", "질문", 7L)
                        .transformDeferred(CircuitBreakerOperator.of(breaker)))
                .expectNext("토큰")
                .verifyComplete();

        assertEquals(0, breaker.getMetrics().getNumberOfFailedCalls());
        assertEquals(1, breaker.getMetrics().getNumberOfSuccessfulCalls());
    }

    // 첫 검색이 0건으로 성공한 뒤 디버그 재검색만 실패해도 답은 이어진다 -> 진단용 호출이라 실패를 서킷에 넘길 이유가 없다
    // 첫 검색 실패와 달리 이미 근거 없음 경로였으므로 폴백 카운터는 오르지 않는다 (2026-11-20 R130-7, s1-embfault 에서 4턴이 이 구멍으로 실패)
    @Test
    @DisplayName("디버그검색실패: 첫 검색이 0건이고 디버그 재검색이 던져도 규칙 프롬프트로 답하고 서킷은 실패를 세지 않으며 폴백 카운터는 0이다")
    void chatStream_디버그검색실패_답은_이어진다() {
        when(chatHistoryProvider.getHistoryBefore("1", 7L)).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of())
                .thenThrow(new IllegalStateException("임베딩 API 장애"));
        when(promptLoader.loadChatRulePrompt()).thenReturn("규칙만");
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> sent = ArgumentCaptor.forClass(List.class);
        when(chatClient.prompt().messages(sent.capture()).stream().chatResponse()).thenReturn(Flux.just(chunk("토큰", "")));

        CircuitBreaker breaker = CircuitBreaker.of("llm-chat-debug-test", CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(1)
                .build());

        StepVerifier.create(service.chatStream("1", "질문", 7L)
                        .transformDeferred(CircuitBreakerOperator.of(breaker)))
                .expectNext("토큰")
                .verifyComplete();

        assertEquals(List.of("규칙만", "질문"), sent.getValue().stream().map(Message::getText).toList());
        verify(vectorStore, times(2)).similaritySearch(any(SearchRequest.class));
        assertEquals(0, breaker.getMetrics().getNumberOfFailedCalls());
        assertEquals(1, breaker.getMetrics().getNumberOfSuccessfulCalls());
        assertEquals(0.0, registry.counter("rag.vector.search.fallback", "path", "chat").count());
    }

    // 검색이 성공하고 0건인 경우는 전과 같다 -> 임계값 때문에 걸러졌는지 보는 디버그 재검색이 이어진다
    @Test
    @DisplayName("검색이 빈 목록을 돌려주면 규칙 프롬프트로 답하고 디버그 재검색을 하며 폴백 카운터는 오르지 않는다")
    void chatStream_검색결과없음_디버그재검색() {
        when(chatHistoryProvider.getHistoryBefore("1", 7L)).thenReturn(List.of());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(promptLoader.loadChatRulePrompt()).thenReturn("규칙만");
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt().messages(any(List.class)).stream().chatResponse()).thenReturn(Flux.just(chunk("토큰", "")));

        StepVerifier.create(service.chatStream("1", "질문", 7L)).expectNext("토큰").verifyComplete();

        verify(vectorStore, times(2)).similaritySearch(any(SearchRequest.class));
        assertEquals(0.0, registry.counter("rag.vector.search.fallback", "path", "chat").count());
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
