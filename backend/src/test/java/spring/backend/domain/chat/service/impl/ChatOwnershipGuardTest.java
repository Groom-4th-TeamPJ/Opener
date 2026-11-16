package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import spring.backend.domain.can.service.spec.CanService;
import spring.backend.domain.chat.dto.message_dto.ChatMessageSaveEvent;
import spring.backend.domain.chat.dto.request.OpenerAnalysisRequest;
import spring.backend.domain.chat.mapper.RedisMessageMapper;
import spring.backend.domain.chat.messaging.ChatMessageConsumer;
import spring.backend.domain.chat.messaging.ChatMessageProducer;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.domain.chat.service.spec.RagService;
import spring.backend.domain.chat.statemachine.ChatSessionEvent;
import spring.backend.domain.chat.statemachine.ChatSessionStateMachineService;
import spring.backend.domain.exam.model.dto.Passage;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.repository.jpa.JpaQuestionRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.domain.user.model.entity.User;
import spring.backend.domain.user.repository.spec.UserRepository;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// 소유권 검증 회귀 테스트 -> 남의 examResultId · questionResultId 를 보내도 통과하던 IDOR 후보를 막는다
// (1) 세션 생성: 프런트가 examResultId 를 sessionId 로 보내는데 exam_result.user_id 와 대조하지 않았다
// (2) 오프너 분석 · 대화 저장 소비자: questionResultId 의 소유자를 확인하지 않았다
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatOwnershipGuardTest {

    private static final Long SESSION_ID = 1L;
    private static final Long QUESTION_RESULT_ID = 10L;
    private static final Long QUESTION_ID = 5L;
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private ChatRedisService chatRedisService;
    @Mock private LlmService llmService;
    @Mock private RagService ragService;
    @Mock private UserRepository userRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatMessageProducer chatMessageProducer;
    @Mock private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    @Mock private JpaQuestionRepository questionRepository;
    @Mock private QuestionResultRepository questionResultRepository;
    @Mock private ExamResultRepository examResultRepository;
    @Mock private CanService canService;
    @Mock private ChatSessionStateMachineService stateMachineService;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private User user;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(
                chatRedisService,
                llmService,
                ragService,
                new ObjectMapper(),
                userRepository,
                new RedisMessageMapper(),
                chatMessageRepository,
                chatMessageProducer,
                redisTemplate,
                questionRepository,
                questionResultRepository,
                examResultRepository,
                canService,
                stateMachineService,
                transactionManager,
                meterRegistry
        );

        when(userRepository.findUserById(USER_ID)).thenReturn(user);
        when(user.getName()).thenReturn("테스터");
        when(stateMachineService.sendEvent(SESSION_ID, ChatSessionEvent.SEND_MESSAGE)).thenReturn(true);
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

    private ChatMessageSaveEvent event() {
        return ChatMessageSaveEvent.builder()
                .sessionId(SESSION_ID)
                .userId(USER_ID)
                .questionResultId(QUESTION_RESULT_ID)
                .publishedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("타인의 examResultId 로 연결하면 거부하고 세션을 만들지 않는다")
    void connectSession_타인의examResultId_거부하고세션을초기화하지않는다() {
        when(examResultRepository.existsByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.connectSession(SESSION_ID, USER_ID));

        // 먼저 연결한 사용자가 소유자가 되는 경로를 연다 -> 403 계열 코드로 끊는다
        assertEquals(ErrorCode.AUTHORIZATION_FAILED, ex.getErrorCode());
        verify(chatRedisService, never()).initializeSession(any(), any());
    }

    @Test
    @DisplayName("본인의 examResultId 로 연결하면 세션을 초기화한다")
    void connectSession_본인의examResultId_통과한다() {
        when(examResultRepository.existsByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(true);

        assertDoesNotThrow(() -> service.connectSession(SESSION_ID, USER_ID));

        verify(chatRedisService).initializeSession(SESSION_ID, USER_ID);
    }

    @Test
    @DisplayName("타인의 questionResultId 로 오프너 분석을 요청하면 거부하고 LLM 과 Can 을 건드리지 않는다")
    void openerAnalysis_타인의questionResultId_거부하고LLM과Can을호출하지않는다() {
        when(examResultRepository.existsByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(true);
        service.connectSession(SESSION_ID, USER_ID);
        when(questionResultRepository.existsByIdAndExamResultUserId(QUESTION_RESULT_ID, USER_ID))
                .thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.openerAnalysis(openerRequest(), USER_ID));

        assertEquals(ErrorCode.AUTHORIZATION_FAILED, ex.getErrorCode());
        // 검증이 비용·생성보다 앞서야 한다 -> 남의 문항 결과로 내 Can 을 쓰고 그 결과에 오프너 표식을 남기는 경로가 닫힌다
        verifyNoInteractions(ragService, canService);
        verify(stateMachineService, never()).sendEvent(SESSION_ID, ChatSessionEvent.SEND_MESSAGE);
    }

    @Test
    @DisplayName("본인의 questionResultId 로 오프너 분석을 요청하면 스트림을 시작한다")
    void openerAnalysis_본인의questionResultId_스트림을시작한다() {
        when(examResultRepository.existsByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(true);
        service.connectSession(SESSION_ID, USER_ID);
        when(questionResultRepository.existsByIdAndExamResultUserId(QUESTION_RESULT_ID, USER_ID))
                .thenReturn(true);
        Question question = mock(Question.class);
        when(question.getPassages()).thenReturn(List.of(new Passage(1, null, "지문")));
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(ragService.generateSimilarProblemStream(any())).thenReturn(Flux.never());

        assertDoesNotThrow(() -> service.openerAnalysis(openerRequest(), USER_ID));

        verify(ragService).generateSimilarProblemStream(any());
    }

    @Test
    @DisplayName("타인의 questionResultId 저장 이벤트는 Redis 를 읽지도 DB 에 쓰지도 않고 DLQ 로 보낸다")
    void handleSaveMessageEvent_타인의questionResultId_저장하지않고DLQ로보낸다() {
        when(questionResultRepository.existsByIdAndExamResultUserId(QUESTION_RESULT_ID, USER_ID))
                .thenReturn(false);

        // 권한 없는 이벤트는 재시도해도 같은 결과 -> 재시도 없이 DLQ 로 보낸다. ACK 로 버리면 원인이 남지 않는다
        AmqpRejectAndDontRequeueException thrown = assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> consumer().handleSaveMessageEvent(event()));
        assertEquals(ErrorCode.AUTHORIZATION_FAILED,
                ((BusinessException) thrown.getCause()).getErrorCode());

        verify(chatRedisService, never()).getSessionMessages(any());
        verify(chatMessageRepository, never()).save(any());
        assertEquals(1.0, meterRegistry.counter("chat.persist.dropped", "reason", "unauthorized").count());
    }

    @Test
    @DisplayName("세션 소유자 불일치 저장 이벤트도 ACK 로 버리지 않고 원인을 단 채 DLQ 로 보낸다")
    void handleSaveMessageEvent_세션소유자불일치_재시도없이DLQ로보낸다() {
        when(questionResultRepository.existsByIdAndExamResultUserId(QUESTION_RESULT_ID, USER_ID)).thenReturn(true);
        when(chatRedisService.getSessionMessages(SESSION_ID)).thenReturn(List.of(
                spring.backend.domain.chat.dto.redis_dto.RedisMessageDto.builder()
                        .chatRole(spring.backend.domain.chat.dto.enums.ChatRole.USER).message("질문").build(),
                spring.backend.domain.chat.dto.redis_dto.RedisMessageDto.builder()
                        .chatRole(spring.backend.domain.chat.dto.enums.ChatRole.LLM).message("답").build()));
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.SESSION_ACCESS_DENIED))
                .when(chatRedisService).validateSessionOwner(SESSION_ID, USER_ID);

        // 소유자가 다른 세션의 이벤트는 재시도해도 같은 결과 -> 문항 결과 소유자 불일치와 같은 경로로 DLQ 에 원인이 남아야 한다
        AmqpRejectAndDontRequeueException thrown = assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> consumer().handleSaveMessageEvent(event()));

        assertEquals(ErrorCode.SESSION_ACCESS_DENIED, ((BusinessException) thrown.getCause()).getErrorCode());
        verify(chatMessageRepository, never()).save(any());
        assertEquals(1.0, meterRegistry.counter("chat.persist.dropped", "reason", "unauthorized").count());
    }

    // 레코드 선언 순서가 (sessionId, questionResultId, questionId) 다
    private OpenerAnalysisRequest openerRequest() {
        return new OpenerAnalysisRequest(SESSION_ID, QUESTION_RESULT_ID, QUESTION_ID);
    }
}
