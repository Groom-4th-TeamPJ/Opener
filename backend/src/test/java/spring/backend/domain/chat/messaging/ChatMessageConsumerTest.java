package spring.backend.domain.chat.messaging;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
import spring.backend.domain.chat.model.entity.ChatMessage;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.chat.dto.message_dto.ChatMessageSaveEvent;
import spring.backend.domain.chat.mapper.RedisMessageMapper;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    @Mock private ChatMessageProducer chatMessageProducer;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private TransactionStatus transactionStatus;
    @Mock private QuestionResult questionResult;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        // 소유권 검증을 통과시킨다 -> 이 클래스의 케이스는 버퍼 판정과 저장 후 요약 이벤트 순서를 보는 테스트다
        lenient().when(questionResultRepository.existsByIdAndExamResultUserId(QUESTION_RESULT_ID, USER_ID))
                .thenReturn(true);
    }

    private ChatMessageConsumer consumer() {
        return new ChatMessageConsumer(
                chatRedisService,
                new RedisMessageMapper(),
                chatMessageRepository,
                questionResultRepository,
                chatMessageProducer,
                meterRegistry,
                transactionTemplate);
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

    private RedisMessageDto llmMessage() {
        return RedisMessageDto.builder()
                .chatRole(ChatRole.LLM)
                .message("답")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private RedisMessageDto userMessage() {
        return RedisMessageDto.builder()
                .chatRole(ChatRole.USER)
                .message("질문")
                .timestamp(LocalDateTime.now())
                .build();
    }

    // TransactionTemplate 목이 콜백을 실제로 실행하게 한다
    private void runTransactionInline() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
    }

    // 저장 호출이 행 id 를 얻은 것처럼 만든다 -> 요약 이벤트는 이 id 를 싣는다
    private void saveAssignsId(Long id) {
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", id);
            return message;
        });
    }

    @Test
    @DisplayName("저장은 요약 없이 끝나고 버퍼 삭제 뒤에 저장된 행 id 로 요약 이벤트 하나를 발행한다")
    void handleSaveMessageEvent_저장후버퍼삭제후요약이벤트를발행한다() {
        when(chatRedisService.getSessionMessages(SESSION_ID)).thenReturn(List.of(userMessage(), llmMessage()));
        when(questionResultRepository.findById(QUESTION_RESULT_ID)).thenReturn(Optional.of(questionResult));
        saveAssignsId(77L);
        runTransactionInline();

        consumer().handleSaveMessageEvent(event(Instant.now()));

        // 순서가 뒤집히면 요약 소비자가 아직 없는 행을 찾는다
        InOrder order = inOrder(transactionTemplate, chatRedisService, chatMessageProducer);
        order.verify(transactionTemplate).execute(any());
        order.verify(chatRedisService).deleteMessage(SESSION_ID);
        order.verify(chatMessageProducer).publishSummaryEvent(77L);
        verify(chatMessageProducer, times(1)).publishSummaryEvent(any());

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(saved.capture());
        // 요약은 이 경로에서 만들지 않는다 -> 요약 소비자가 채울 때까지 null
        assertNull(saved.getValue().getSummary());
    }

    @Test
    @DisplayName("요약 이벤트 발행이 실패해도 저장은 성공으로 끝나고 실패는 메트릭에 남는다")
    void handleSaveMessageEvent_요약이벤트발행실패_저장은성공한다() {
        when(chatRedisService.getSessionMessages(SESSION_ID)).thenReturn(List.of(userMessage(), llmMessage()));
        when(questionResultRepository.findById(QUESTION_RESULT_ID)).thenReturn(Optional.of(questionResult));
        saveAssignsId(77L);
        runTransactionInline();
        doThrow(new AmqpConnectException(new RuntimeException("broker down")))
                .when(chatMessageProducer).publishSummaryEvent(77L);

        // 행은 이미 저장됐고 버퍼도 지웠다 -> 저장 이벤트를 재시도하면 빈 버퍼로 CHAT_BUFFER_LOST 가 되어 DLQ 로만 간다
        assertDoesNotThrow(() -> consumer().handleSaveMessageEvent(event(Instant.now())));

        assertEquals(1.0, meterRegistry.counter("chat.summary.publish_failed").count());
    }

    // 요약 읽기는 소비자가 영구 저장에서 뺀 턴을 같은 규칙으로 뺀다
    // (이 테스트가 여기 있는 이유: 어댑터 전용 테스트 파일은 이 작업의 파일 표 밖이다)
    @Test
    @DisplayName("요약이 읽는 전량 히스토리는 끊긴 응답과 답 없는 질문을 제외한다")
    void getFullHistory_끊긴응답과답없는질문_요약읽기에서제외한다() {
        RedisMessageDto partial = RedisMessageDto.builder()
                .chatRole(ChatRole.LLM).message("끊긴 답").timestamp(LocalDateTime.now()).partial(true).build();
        RedisMessageDto unansweredTail = RedisMessageDto.builder()
                .chatRole(ChatRole.USER).message("답 없는 마지막 질문").timestamp(LocalDateTime.now()).build();
        when(chatRedisService.getSessionMessages(SESSION_ID)).thenReturn(List.of(
                userMessage(), partial, userMessage(), llmMessage(), unansweredTail));

        List<org.springframework.ai.chat.messages.Message> history =
                new spring.backend.domain.chat.adapter.RedisChatHistoryAdapter(chatRedisService)
                        .getFullHistory(SESSION_ID.toString());

        // 요약은 영구 저장되므로 저장본과 다른 대화를 요약하면 복습 화면의 요약이 본문과 어긋난다
        assertEquals(List.of("질문", "답"),
                history.stream().map(org.springframework.ai.chat.messages.Message::getText).toList());
    }
}
