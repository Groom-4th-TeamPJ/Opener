package spring.backend.domain.chat.service.impl;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
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
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// RAG 를 끈 구현도 같은 계약을 따른다 -> 저장 길이로 읽은 히스토리 + 현재 질문 한 번
@ExtendWith(MockitoExtension.class)
class PlainLlmServiceTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private ChatHistoryProvider chatHistoryProvider;
    @Mock private PromptLoader promptLoader;

    private PlainLlmService service;
    private ChatClient chatClient;
    private ArgumentCaptor<List<Message>> sent;
    // 시험이 응답을 바꿔 끼운다 -> setUp 의 스텁은 구독 시점에 이 값을 읽는다
    private Flux<ChatResponse> responses = Flux.just(chunk("토큰", "end_turn"));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new PlainLlmService(chatClientBuilder, chatHistoryProvider, promptLoader);
        when(promptLoader.loadChatRulePrompt()).thenReturn("시스템 규칙");
        chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        sent = ArgumentCaptor.forClass(List.class);
        when(chatClient.prompt().system(anyString()).messages(sent.capture()).stream().chatResponse())
                .thenReturn(Flux.defer(() -> responses));
    }

    @Test
    @DisplayName("저장 길이로 읽은 히스토리 뒤에 현재 질문을 정확히 한 번 붙여 보낸다")
    void chatStream_저장길이로읽은히스토리_뒤에현재질문한번() {
        when(chatHistoryProvider.getHistoryBefore("1", 7L)).thenReturn(List.of(
                new UserMessage("이전 질문"), new AssistantMessage("이전 답")));

        StepVerifier.create(service.chatStream("1", "현재 질문", 7L)).expectNext("토큰").expectComplete().verify(TIMEOUT);

        List<Message> messages = sent.getValue();
        assertEquals(List.of("이전 질문", "이전 답", "현재 질문"), messages.stream().map(Message::getText).toList());
        assertInstanceOf(UserMessage.class, messages.get(2));
        verify(chatHistoryProvider).getHistoryBefore("1", 7L);
    }

    @Test
    @DisplayName("저장이 버려진 턴(-1)도 그 값으로 히스토리를 읽고 질문은 한 번만 실린다")
    void chatStream_저장버려짐_마이너스1로읽고질문은한번() {
        when(chatHistoryProvider.getHistoryBefore("1", -1L)).thenReturn(List.of(
                new UserMessage("이전 질문"), new AssistantMessage("이전 답")));

        StepVerifier.create(service.chatStream("1", "현재 질문", -1L)).expectNext("토큰").expectComplete().verify(TIMEOUT);

        assertEquals(List.of("이전 질문", "이전 답", "현재 질문"),
                sent.getValue().stream().map(Message::getText).toList());
        verify(chatHistoryProvider).getHistoryBefore("1", -1L);
    }

    @Test
    @DisplayName("이전 질문과 글자가 같은 질문을 다시 보내도 현재 질문이 빠지지 않는다")
    void chatStream_이전질문과같은질문_현재질문이실린다() {
        // 히스토리에는 현재 질문이 없으므로 같은 글자의 이전 질문이 있어도 덧붙여야 모델이 이번 턴에 답한다
        when(chatHistoryProvider.getHistoryBefore("1", 3L)).thenReturn(List.of(
                new UserMessage("계속"), new AssistantMessage("첫 답")));

        StepVerifier.create(service.chatStream("1", "계속", 3L)).expectNext("토큰").expectComplete().verify(TIMEOUT);

        assertEquals(List.of("계속", "첫 답", "계속"), sent.getValue().stream().map(Message::getText).toList());
    }

    @Test
    @DisplayName("stop_reason 이 refusal 이면 LLM 응답 실패 에러로 끝난다")
    void chatStream_refusal_LLM실패에러() {
        when(chatHistoryProvider.getHistoryBefore("1", 3L)).thenReturn(List.of());
        responses = Flux.just(chunk("", "refusal"));

        StepVerifier.create(service.chatStream("1", "질문", 3L))
                .expectErrorSatisfies(e -> assertEquals(ErrorCode.LLM_RESPONSE_FAIL, ((BusinessException) e).getErrorCode()))
                .verify(TIMEOUT);
    }

    @Test
    @DisplayName("텍스트가 한 조각도 없이 끝나면 빈 완료 대신 LLM 응답 실패 에러다")
    void chatStream_텍스트없음_LLM실패에러() {
        when(chatHistoryProvider.getHistoryBefore("1", 3L)).thenReturn(List.of());
        responses = Flux.empty();

        StepVerifier.create(service.chatStream("1", "질문", 3L))
                .expectError(BusinessException.class)
                .verify(TIMEOUT);
    }

    // 토큰 사이 간격 상한(app.llm.response-timeout 20초) -> 멈춘 스트림이 SSE sink 를 180초 전체 상한까지 붙잡지 않는다
    @Test
    @DisplayName("첫 조각 뒤 20초 동안 아무것도 안 오면 타임아웃 에러로 끝난다")
    void chatStream_조각뒤20초무응답_타임아웃() {
        when(chatHistoryProvider.getHistoryBefore("1", 3L)).thenReturn(List.of());
        responses = Flux.just(chunk("첫", "")).concatWith(Flux.never());

        StepVerifier.withVirtualTime(() -> service.chatStream("1", "질문", 3L))
                .expectNext("첫")
                .expectNoEvent(Duration.ofSeconds(19))
                .thenAwait(Duration.ofSeconds(1))
                .expectError(TimeoutException.class)
                .verify(TIMEOUT);
    }

    @Test
    @DisplayName("첫 조각도 20초 안에 오지 않으면 타임아웃 에러로 끝난다")
    void chatStream_첫조각20초무응답_타임아웃() {
        when(chatHistoryProvider.getHistoryBefore("1", 3L)).thenReturn(List.of());
        responses = Flux.never();

        StepVerifier.withVirtualTime(() -> service.chatStream("1", "질문", 3L))
                .thenAwait(Duration.ofSeconds(20))
                .expectError(TimeoutException.class)
                .verify(TIMEOUT);
    }

    @Test
    @DisplayName("조각 사이가 19초씩 벌어져도 간격 상한 안이므로 끝까지 완료된다")
    void chatStream_19초간격_완료() {
        when(chatHistoryProvider.getHistoryBefore("1", 3L)).thenReturn(List.of());
        responses = Flux.defer(() -> Flux.just(chunk("가", ""), chunk("나", ""), chunk("", "end_turn"))
                .delayElements(Duration.ofSeconds(19)));

        StepVerifier.withVirtualTime(() -> service.chatStream("1", "질문", 3L))
                .thenAwait(Duration.ofSeconds(60))
                .expectNext("가", "나")
                .expectComplete()
                .verify(TIMEOUT);
    }

    private static ChatResponse chunk(String text, String finishReason) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text),
                ChatGenerationMetadata.builder().finishReason(finishReason).build())));
    }
}
