package spring.backend.domain.chat.adapter;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
import spring.backend.domain.chat.service.spec.ChatRedisService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 조회 범위 회귀 테스트
// getHistoryBefore() 가 전량을 읽으면 프롬프트 비용이 선형 증가한다 -> 질문 직전까지의 윈도우 읽기로 고정한다
@ExtendWith(MockitoExtension.class)
class RedisChatHistoryAdapterTest {

    private static final Long SESSION_ID = 7L;
    private static final String CONVERSATION_ID = "7";

    @Mock private ChatRedisService chatRedisService;

    @InjectMocks
    private RedisChatHistoryAdapter adapter;

    @Test
    @DisplayName("getHistoryBefore() 는 저장 길이를 서비스에 넘겨 질문 직전까지만 조회한다")
    void getHistoryBefore_usesWrittenLength() {
        when(chatRedisService.getHistoryBefore(SESSION_ID, 5L)).thenReturn(messages());

        List<Message> result = adapter.getHistoryBefore(CONVERSATION_ID, 5L);

        assertEquals(2, result.size());
        assertInstanceOf(UserMessage.class, result.get(0));
        assertInstanceOf(AssistantMessage.class, result.get(1));

        verify(chatRedisService).getHistoryBefore(SESSION_ID, 5L);
        verify(chatRedisService, never()).getSessionMessages(SESSION_ID);
    }

    @Test
    @DisplayName("getHistoryBefore() 는 conversationId 가 숫자가 아니면 조회 없이 빈 목록을 반환한다")
    void getHistoryBefore_invalidConversationId_returnsEmpty() {
        assertTrue(adapter.getHistoryBefore("not-a-number", 5L).isEmpty());

        verify(chatRedisService, never()).getHistoryBefore(anyLong(), anyLong());
    }

    @Test
    @DisplayName("getHistoryBefore() 는 서비스가 예외를 던져도 빈 목록으로 흡수한다")
    void getHistoryBefore_serviceThrows_returnsEmpty() {
        when(chatRedisService.getHistoryBefore(SESSION_ID, 5L)).thenThrow(new IllegalStateException("redis down"));

        assertTrue(adapter.getHistoryBefore(CONVERSATION_ID, 5L).isEmpty());
    }

    private List<RedisMessageDto> messages() {
        return List.of(
                RedisMessageDto.builder()
                        .chatRole(ChatRole.USER)
                        .message("질문")
                        .timestamp(LocalDateTime.now())
                        .build(),
                RedisMessageDto.builder()
                        .chatRole(ChatRole.LLM)
                        .message("답변")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
