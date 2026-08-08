package spring.backend.domain.chat.messaging;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.backend.domain.chat.dto.message_dto.ChatMessageSaveEvent;
import spring.backend.domain.chat.mapper.RedisMessageMapper;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

// 버퍼가 비었을 때 정상 만료와 비정상 소실을 구분하지 못하면 유실을 유실로 알 수 없다
@ExtendWith(MockitoExtension.class)
class ChatMessageConsumerTest {

    private static final Long SESSION_ID = 1L;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Long QUESTION_RESULT_ID = 10L;

    @Mock private ChatRedisService chatRedisService;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private QuestionResultRepository questionResultRepository;
    @Mock private LlmService llmService;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private ChatMessageConsumer consumer() {
        return new ChatMessageConsumer(
                chatRedisService,
                new RedisMessageMapper(),
                chatMessageRepository,
                questionResultRepository,
                llmService,
                meterRegistry);
    }

    private ChatMessageSaveEvent event(Instant publishedAt) {
        return ChatMessageSaveEvent.builder()
                .sessionId(SESSION_ID)
                .userId(USER_ID)
                .questionResultId(QUESTION_RESULT_ID)
                .publishedAt(publishedAt)
                .build();
    }

    @Test
    @DisplayName("TTL 을 넘긴 이벤트에서 버퍼가 비면 정상 만료로 보고 ACK 한다")
    void handleSaveMessageEvent_TTL초과_정상만료로처리한다() {
        when(chatRedisService.getSessionMessages(SESSION_ID)).thenReturn(List.of());

        Instant old = Instant.now().minus(Duration.ofHours(2));
        assertDoesNotThrow(() -> consumer().handleSaveMessageEvent(event(old)));

        assertEquals(1.0,
                meterRegistry.counter("chat.persist.dropped", "reason", "expired").count());
    }

    @Test
    @DisplayName("TTL 안인데 버퍼가 비면 예외를 던져 DLQ 로 보낸다")
    void handleSaveMessageEvent_TTL이내_비정상소실은예외를던진다() {
        when(chatRedisService.getSessionMessages(SESSION_ID)).thenReturn(List.of());

        // 5초 전에 발행됐는데 버퍼가 비었다 = 복제 유실·LRU 축출. 정상 만료로 설명되지 않는다
        Instant fresh = Instant.now().minus(Duration.ofSeconds(5));
        BusinessException thrown = assertThrows(BusinessException.class,
                () -> consumer().handleSaveMessageEvent(event(fresh)));

        assertEquals(ErrorCode.CHAT_BUFFER_LOST, thrown.getErrorCode());
        assertEquals(1.0,
                meterRegistry.counter("chat.persist.dropped", "reason", "lost").count());
    }

    @Test
    @DisplayName("publishedAt 이 없는 구 메시지는 안전한 쪽인 정상 만료로 본다")
    void handleSaveMessageEvent_발행시각없음_정상만료로처리한다() {
        when(chatRedisService.getSessionMessages(SESSION_ID)).thenReturn(List.of());

        // 배포 전에 큐에 실린 메시지는 판정 근거가 없다. DLQ 로 보내면 배포 순간 대량 유입된다
        assertDoesNotThrow(() -> consumer().handleSaveMessageEvent(event(null)));

        assertEquals(1.0,
                meterRegistry.counter("chat.persist.dropped", "reason", "expired").count());
    }
}
