package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import spring.backend.domain.can.service.spec.CanService;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.message_dto.ChatMessageSaveEvent;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
import spring.backend.domain.chat.dto.request.OpenerAnalysisRequest;
import spring.backend.domain.chat.mapper.RedisMessageMapper;
import spring.backend.domain.chat.messaging.ChatMessageConsumer;
import spring.backend.domain.chat.messaging.ChatMessageProducer;
import spring.backend.domain.chat.model.entity.ChatMessage;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.domain.chat.service.spec.RagService;
import spring.backend.domain.chat.statemachine.ChatSessionEvent;
import spring.backend.domain.chat.statemachine.ChatSessionStateMachineService;
import spring.backend.domain.exam.model.dto.Passage;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.exam.repository.jpa.JpaQuestionRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.domain.user.model.entity.User;
import spring.backend.domain.user.repository.spec.UserRepository;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 실패한 턴에 과금하지 않고, 잘린 응답을 완결 답변처럼 영구 저장하지 않는다는 계약
// (1) openerAnalysis: Can 차감이 saveMessage 보다 앞서 저장 실패 턴도 과금됐다
// (2) ChatMessageConsumer: 빈 버퍼만 감지해 중간에 끊긴 응답이 완결 답변으로 DB 에 들어갔다
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpenerAnalysisCanOrderTest {

    private static final Long SESSION_ID = 1L;
    private static final Long QUESTION_ID = 5L;
    private static final Long QUESTION_RESULT_ID = 10L;
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
    @Mock private TransactionStatus transactionStatus;
    @Mock private QuestionResult questionResult;
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
        when(examResultRepository.existsByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(true);
        when(questionResultRepository.existsByIdAndExamResultUserId(QUESTION_RESULT_ID, USER_ID)).thenReturn(true);
    }

    private ChatMessageConsumer consumer() {
        return new ChatMessageConsumer(
                chatRedisService,
                new RedisMessageMapper(),
                chatMessageRepository,
                questionResultRepository,
                llmService,
                meterRegistry,
                transactionTemplate);
    }

    // 연결된 세션과 분석할 문제를 준비한다 -> openerAnalysis 가 스트림 시작 직전까지 가는 경로
    private void readyForOpenerAnalysis() {
        service.connectSession(SESSION_ID, USER_ID);
        Question question = mock(Question.class);
        when(question.getPassages()).thenReturn(List.of(new Passage(1, null, "지문")));
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(ragService.generateSimilarProblemStream(any())).thenReturn(Flux.never());
    }

    private OpenerAnalysisRequest openerRequest() {
        return new OpenerAnalysisRequest(SESSION_ID, QUESTION_RESULT_ID, QUESTION_ID);
    }

    @Test
    @DisplayName("분석 요청 메시지를 저장한 뒤에 Can 을 차감한다")
    void openerAnalysis_정상_저장한뒤에Can을차감한다() {
        readyForOpenerAnalysis();

        service.openerAnalysis(openerRequest(), USER_ID);

        // 순서가 뒤집히면 저장에 실패한 턴에도 Can 이 빠진다
        InOrder order = inOrder(chatRedisService, canService);
        order.verify(chatRedisService).saveMessage(eq(SESSION_ID), any(RedisMessageDto.class));
        order.verify(canService).useUserCan(USER_ID, 1);
    }

    @Test
    @DisplayName("저장이 실패하면 Can 을 차감하지 않고 상태를 회수한다")
    void openerAnalysis_저장실패_Can을차감하지않는다() {
        readyForOpenerAnalysis();
        doThrow(new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL))
                .when(chatRedisService).saveMessage(eq(SESSION_ID), any(RedisMessageDto.class));

        assertThrows(BusinessException.class, () -> service.openerAnalysis(openerRequest(), USER_ID));

        verify(canService, never()).useUserCan(any(), anyInt());
        // 상태를 되돌리지 않으면 그 세션은 재연결 전까지 모든 메시지가 SESSION_EXPIRED 로 거부된다
        verify(stateMachineService).sendEvent(SESSION_ID, ChatSessionEvent.STREAM_ERROR);
    }

    @Test
    @DisplayName("끝에 partial 표식이 붙은 잘린 응답은 완결 답변으로 저장하지 않는다")
    void handleSaveMessageEvent_잘린버퍼_완결응답으로저장하지않는다() {
        when(chatRedisService.getSessionMessages(SESSION_ID))
                .thenReturn(List.of(message(ChatRole.USER, "질문", false), message(ChatRole.LLM, "중간에 끊긴 답", true)));
        when(questionResultRepository.findById(QUESTION_RESULT_ID)).thenReturn(Optional.of(questionResult));
        runTransactionInline();

        consumer().handleSaveMessageEvent(event());

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(saved.capture());
        // 잘린 답이 영구 기록에 완결 답변으로 남으면 복습 화면이 끊긴 문장을 정답 풀이처럼 보여 준다
        assertTrue(saved.getValue().getMessages().stream().noneMatch(m -> "중간에 끊긴 답".equals(m.getContent())),
                "잘린 응답이 저장됨");
        assertEquals(1.0, meterRegistry.counter("chat.persist.dropped", "reason", "truncated").count());
    }

    @Test
    @DisplayName("표식 없는 완결 버퍼는 그대로 저장한다")
    void handleSaveMessageEvent_완결버퍼_그대로저장한다() {
        when(chatRedisService.getSessionMessages(SESSION_ID))
                .thenReturn(List.of(message(ChatRole.USER, "질문", false), message(ChatRole.LLM, "완결된 답", false)));
        when(questionResultRepository.findById(QUESTION_RESULT_ID)).thenReturn(Optional.of(questionResult));
        runTransactionInline();

        consumer().handleSaveMessageEvent(event());

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(saved.capture());
        assertEquals(2, saved.getValue().getMessages().size());
    }

    private RedisMessageDto message(ChatRole role, String text, boolean partial) {
        return RedisMessageDto.builder()
                .chatRole(role)
                .message(text)
                .timestamp(LocalDateTime.now())
                .partial(partial)
                .build();
    }

    private ChatMessageSaveEvent event() {
        return ChatMessageSaveEvent.builder()
                .sessionId(SESSION_ID)
                .userId(USER_ID)
                .questionResultId(QUESTION_RESULT_ID)
                .publishedAt(Instant.now())
                .build();
    }

    // TransactionTemplate 목이 콜백을 실제로 실행하게 한다
    private void runTransactionInline() {
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
    }
}
