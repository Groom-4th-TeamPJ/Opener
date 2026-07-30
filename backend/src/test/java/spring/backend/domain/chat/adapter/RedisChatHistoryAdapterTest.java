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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 조회 범위 회귀 테스트
// getRecentHistory() 가 전량을 읽으면 프롬프트 비용이 선형 증가하고, getFullHistory() 가 윈도우를 읽으면
// 요약이 잘린 채 PostgreSQL 에 영구 저장된다 -> 두 방향 모두 고정한다
@ExtendWith(MockitoExtension.class)
class RedisChatHistoryAdapterTest {

    private static final Long SESSION_ID = 7L;
    private static final String CONVERSATION_ID = "7";

    @Mock private ChatRedisService chatRedisService;

    @InjectMocks
    private RedisChatHistoryAdapter adapter;

    @Test
    @DisplayName("getRecentHistory() 은 윈도우 적용본만 조회한다")
    void get_usesRecentWindow() {
        when(chatRedisService.getRecentSessionMessages(SESSION_ID)).thenReturn(messages());

        List<Message> result = adapter.getRecentHistory(CONVERSATION_ID);

        assertEquals(2, result.size());
        assertInstanceOf(UserMessage.class, result.get(0));
        assertInstanceOf(AssistantMessage.class, result.get(1));

        verify(chatRedisService).getRecentSessionMessages(SESSION_ID);
        verify(chatRedisService, never()).getSessionMessages(SESSION_ID);
    }

    @Test
    @DisplayName("getFullHistory() 는 윈도우 없이 전량을 조회한다")
    void getFullHistory_usesAllMessages() {
        when(chatRedisService.getSessionMessages(SESSION_ID)).thenReturn(messages());

        List<Message> result = adapter.getFullHistory(CONVERSATION_ID);

        assertEquals(2, result.size());

        verify(chatRedisService).getSessionMessages(SESSION_ID);
        verify(chatRedisService, never()).getRecentSessionMessages(SESSION_ID);
    }

    @Test
    @DisplayName("conversationId 가 숫자가 아니면 조회 없이 빈 목록을 반환한다")
    void getFullHistory_invalidConversationId_returnsEmpty() {
        assertTrue(adapter.getFullHistory("not-a-number").isEmpty());

        verify(chatRedisService, never()).getSessionMessages(SESSION_ID);
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
