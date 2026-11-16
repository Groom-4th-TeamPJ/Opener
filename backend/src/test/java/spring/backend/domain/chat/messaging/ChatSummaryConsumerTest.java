package spring.backend.domain.chat.messaging;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.message_dto.ChatSummaryEvent;
import spring.backend.domain.chat.model.entity.ChatMessage;
import spring.backend.domain.chat.model.entity.ChatMessageContent;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 저장은 요약 없이 끝나므로 요약은 저장된 행을 읽어 따로 채운다
// 같은 이벤트가 다시 배달돼도 LLM 을 또 부르지 않고, 영구 실패와 일시 실패는 재시도 규칙이 다르다
@ExtendWith(MockitoExtension.class)
class ChatSummaryConsumerTest {

    private static final Long CHAT_MESSAGE_ID = 77L;

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private LlmService llmService;

    private ChatSummaryConsumer consumer() {
        return new ChatSummaryConsumer(chatMessageRepository, llmService);
    }

    private ChatMessage row(String summary) {
        return ChatMessage.createFromSession(null, List.of(
                ChatMessageContent.builder().role(ChatRole.USER).content("질문").timestamp(LocalDateTime.now()).build(),
                ChatMessageContent.builder().role(ChatRole.LLM).content("답").timestamp(LocalDateTime.now()).build()),
                summary);
    }

    @Test
    @DisplayName("요약이 없는 행은 저장된 메시지를 역할대로 변환해 요약하고 그 요약을 저장한다")
    void handleSummaryEvent_요약없는행_저장된메시지로요약해저장한다() {
        ChatMessage row = row(null);
        when(chatMessageRepository.findById(CHAT_MESSAGE_ID)).thenReturn(Optional.of(row));
        when(llmService.summarize(any())).thenReturn("요약문");

        consumer().handleSummaryEvent(new ChatSummaryEvent(CHAT_MESSAGE_ID));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Message>> input = ArgumentCaptor.forClass(List.class);
        verify(llmService).summarize(input.capture());
        // Redis 버퍼는 이미 지워졌다 -> 입력은 행에 저장된 메시지이고 역할 매핑은 어댑터와 같다
        assertEquals(2, input.getValue().size());
        assertEquals(UserMessage.class, input.getValue().get(0).getClass());
        assertEquals("질문", input.getValue().get(0).getText());
        assertEquals(AssistantMessage.class, input.getValue().get(1).getClass());
        assertEquals("답", input.getValue().get(1).getText());

        verify(chatMessageRepository).save(row);
        assertEquals("요약문", row.getSummary());
    }

    @Test
    @DisplayName("이미 요약이 있는 행은 LLM 을 부르지도 다시 저장하지도 않는다")
    void handleSummaryEvent_이미요약됨_LLM을부르지않는다() {
        when(chatMessageRepository.findById(CHAT_MESSAGE_ID)).thenReturn(Optional.of(row("옛 요약")));

        consumer().handleSummaryEvent(new ChatSummaryEvent(CHAT_MESSAGE_ID));

        // 재배달마다 4.5 초짜리 호출을 되풀이하면 중복 이벤트가 LLM 부하를 그대로 늘린다
        verify(llmService, never()).summarize(any());
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("행이 없으면 재시도 없이 DLQ 로 보내는 예외를 던진다")
    void handleSummaryEvent_행없음_재시도없이DLQ로보낸다() {
        when(chatMessageRepository.findById(CHAT_MESSAGE_ID)).thenReturn(Optional.empty());

        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> consumer().handleSummaryEvent(new ChatSummaryEvent(CHAT_MESSAGE_ID)));

        verify(llmService, never()).summarize(any());
    }

    @Test
    @DisplayName("행 id 가 빠진 이벤트는 재시도 없이 DLQ 로 보내는 예외를 던진다")
    void handleSummaryEvent_id없음_재시도없이DLQ로보낸다() {
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> consumer().handleSummaryEvent(new ChatSummaryEvent(null)));
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer().handleSummaryEvent(null));
    }

    @Test
    @DisplayName("LLM 이 요약을 돌려주지 못하면 재시도가 걸리는 일반 예외를 던지고 저장하지 않는다")
    void handleSummaryEvent_LLM요약없음_재시도가능한예외를던진다() {
        ChatMessage row = row(null);
        when(chatMessageRepository.findById(CHAT_MESSAGE_ID)).thenReturn(Optional.of(row));
        when(llmService.summarize(any())).thenReturn(null);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> consumer().handleSummaryEvent(new ChatSummaryEvent(CHAT_MESSAGE_ID)));

        // 거절 예외로 바꾸면 LLM 이 잠시 막힌 한 번으로 요약이 DLQ 로 간다
        assertFalse(thrown instanceof AmqpRejectAndDontRequeueException);
        assertNull(row.getSummary());
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("LLM 이 던진 도메인 거절은 그대로 올라가 저장하지 않는다")
    void handleSummaryEvent_LLM예외_그대로던지고저장하지않는다() {
        ChatMessage row = row(null);
        BusinessException rejection = new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);
        when(chatMessageRepository.findById(CHAT_MESSAGE_ID)).thenReturn(Optional.of(row));
        when(llmService.summarize(any())).thenThrow(rejection);

        assertSame(rejection, assertThrows(BusinessException.class,
                () -> consumer().handleSummaryEvent(new ChatSummaryEvent(CHAT_MESSAGE_ID))));

        verify(chatMessageRepository, never()).save(any());
    }
}
