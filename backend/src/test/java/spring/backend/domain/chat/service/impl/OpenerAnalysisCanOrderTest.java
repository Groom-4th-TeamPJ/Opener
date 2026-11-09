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
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
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
    @DisplayName("끝에 partial 표식이 붙은 잘린 응답은 그 질문과 함께 완결 답변으로 저장하지 않는다")
    void handleSaveMessageEvent_잘린버퍼_완결응답으로저장하지않는다() {
        when(chatRedisService.getSessionMessages(SESSION_ID)).thenReturn(List.of(
                message(ChatRole.USER, "질문", false), message(ChatRole.LLM, "첫 답", false),
                message(ChatRole.USER, "두번째 질문", false), message(ChatRole.LLM, "중간에 끊긴 답", true)));
        when(questionResultRepository.findById(QUESTION_RESULT_ID)).thenReturn(Optional.of(questionResult));
        runTransactionInline();

        consumer().handleSaveMessageEvent(event());

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(saved.capture());
        // 잘린 답이 영구 기록에 완결 답변으로 남으면 복습 화면이 끊긴 문장을 정답 풀이처럼 보여 준다
        // 답을 받지 못한 질문만 남는 것도 같은 이유로 뺀다 -> 앞의 완결된 한 턴만 남는다
        assertEquals(List.of("질문", "첫 답"),
                saved.getValue().getMessages().stream().map(m -> m.getContent()).toList());
        assertEquals(2.0, meterRegistry.counter("chat.persist.dropped", "reason", "truncated").count());
    }

    @Test
    @DisplayName("끝이 답 없는 사용자 메시지인 버퍼는 그 질문을 저장하지 않는다")
    void handleSaveMessageEvent_끝이사용자메시지_답없는턴을저장하지않는다() {
        // 첫 청크 전에 끊겼거나 아직 스트리밍 중인 턴이다
        when(chatRedisService.getSessionMessages(SESSION_ID)).thenReturn(List.of(
                message(ChatRole.USER, "질문", false), message(ChatRole.LLM, "답", false),
                message(ChatRole.USER, "답을 못 받은 질문", false)));
        when(questionResultRepository.findById(QUESTION_RESULT_ID)).thenReturn(Optional.of(questionResult));
        runTransactionInline();

        consumer().handleSaveMessageEvent(event());

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(saved.capture());
        assertEquals(2, saved.getValue().getMessages().size());
        assertEquals(1.0, meterRegistry.counter("chat.persist.dropped", "reason", "truncated").count());
    }

    @Test
    @DisplayName("답이 하나도 없는 버퍼는 DB 에 쓰지도 요약하지도 않는다")
    void handleSaveMessageEvent_답이하나도없는버퍼_저장하지않는다() {
        when(chatRedisService.getSessionMessages(SESSION_ID))
                .thenReturn(List.of(message(ChatRole.USER, "질문", false)));

        consumer().handleSaveMessageEvent(event());

        verify(chatMessageRepository, never()).save(any());
        verify(llmService, never()).summaryChat(any());
    }

    @Test
    @DisplayName("문항 결과가 없는 영구 실패는 재시도 없이 DLQ 로 보내는 예외를 던진다")
    void handleSaveMessageEvent_문항결과없음_재시도없이DLQ로보낸다() {
        when(chatRedisService.getSessionMessages(SESSION_ID)).thenReturn(List.of(
                message(ChatRole.USER, "질문", false), message(ChatRole.LLM, "답", false)));
        when(questionResultRepository.findById(QUESTION_RESULT_ID)).thenReturn(Optional.empty());
        runTransactionInline();

        // 재시도해도 같은 결과인데 재시도 횟수만큼 summaryChat 을 다시 부른다
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer().handleSaveMessageEvent(event()));
    }

    @Test
    @DisplayName("Redis 읽기 실패는 재시도가 걸리도록 일반 예외로 던진다")
    void handleSaveMessageEvent_Redis읽기실패_재시도가능한예외를던진다() {
        when(chatRedisService.getSessionMessages(SESSION_ID))
                .thenThrow(new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL));

        // 일시 장애는 재시도로 회복될 수 있다 -> 재시도 불가 예외로 바꾸면 한 번의 장애로 메시지가 DLQ 로 간다
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> consumer().handleSaveMessageEvent(event()));
        assertTrue(thrown instanceof BusinessException, "재시도 가능한 BusinessException 이어야 함: " + thrown);
    }

    @Test
    @DisplayName("환불이 실패해도 상태 회수는 건너뛰지 않고 원래 예외를 던진다")
    void openerAnalysis_환불실패_상태는회수하고원래예외를던진다() {
        readyForOpenerAnalysis();
        when(ragService.generateSimilarProblemStream(any())).thenThrow(new IllegalStateException("벡터 검색 실패"));
        when(canService.recoverUserCan(USER_ID, 1)).thenThrow(new IllegalArgumentException("환불 실패"));

        // 환불 예외가 상태 회수를 건너뛰게 하면 그 세션은 재연결 전까지 모든 메시지가 SESSION_EXPIRED 로 거부된다
        assertThrows(IllegalStateException.class, () -> service.openerAnalysis(openerRequest(), USER_ID));

        verify(stateMachineService).sendEvent(SESSION_ID, ChatSessionEvent.STREAM_ERROR);
    }

    @Test
    @DisplayName("사용자 메시지가 연달아 있으면 답을 받지 못한 앞의 질문은 저장하지 않는다")
    void handleSaveMessageEvent_사용자메시지연달음_답없는질문을저장하지않는다() {
        // 첫 답이 partial 로 빠지면 U1 뒤에 바로 U2 가 와서 끝이 아닌 자리에도 답 없는 질문이 남는다
        when(chatRedisService.getSessionMessages(SESSION_ID)).thenReturn(List.of(
                message(ChatRole.USER, "답을 못 받은 질문", false), message(ChatRole.LLM, "끊긴 답", true),
                message(ChatRole.USER, "두번째 질문", false), message(ChatRole.LLM, "완결된 답", false)));
        when(questionResultRepository.findById(QUESTION_RESULT_ID)).thenReturn(Optional.of(questionResult));
        runTransactionInline();

        consumer().handleSaveMessageEvent(event());

        ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(saved.capture());
        assertEquals(List.of("두번째 질문", "완결된 답"),
                saved.getValue().getMessages().stream().map(m -> m.getContent()).toList());
        assertEquals(2.0, meterRegistry.counter("chat.persist.dropped", "reason", "truncated").count());
    }

    @Test
    @DisplayName("필수 필드가 빠진 저장 이벤트는 재시도 없이 DLQ 로 보낸다")
    void handleSaveMessageEvent_필수필드없음_재시도없이DLQ로보낸다() {
        ChatMessageSaveEvent broken = ChatMessageSaveEvent.builder()
                .sessionId(SESSION_ID)
                .questionResultId(QUESTION_RESULT_ID)
                .publishedAt(Instant.now())
                .build();

        // 페이로드가 잘못된 메시지는 몇 번을 다시 읽어도 같은 결과다
        assertThrows(AmqpRejectAndDontRequeueException.class, () -> consumer().handleSaveMessageEvent(broken));

        // 필드 검사를 건너뛰면 소유권 조회가 같은 거절 예외로 우연히 끝난다 -> 조회와 읽기가 모두 일어나지 않았음을 본다
        verify(questionResultRepository, never()).existsByIdAndExamResultUserId(any(), any());
        verify(chatRedisService, never()).getSessionMessages(any());
        assertEquals(1.0, meterRegistry.counter("chat.persist.dropped", "reason", "invalid").count());
    }

    @Test
    @DisplayName("스트림 조립이 예외로 끝나면 차감한 Can 을 환불하고 상태를 회수한다")
    void openerAnalysis_스트림조립예외_Can을환불하고상태를회수한다() {
        readyForOpenerAnalysis();
        when(ragService.generateSimilarProblemStream(any())).thenThrow(new IllegalStateException("벡터 검색 실패"));

        assertThrows(IllegalStateException.class, () -> service.openerAnalysis(openerRequest(), USER_ID));

        // 조립 시점 예외는 doOnError 를 타지 않아 환불과 상태 회수가 모두 건너뛰어진다
        InOrder order = inOrder(canService);
        order.verify(canService).useUserCan(USER_ID, 1);
        order.verify(canService).recoverUserCan(USER_ID, 1);
        verify(stateMachineService).sendEvent(SESSION_ID, ChatSessionEvent.STREAM_ERROR);
    }

    @Test
    @DisplayName("Can 이 부족하면 답 없이 남는 요청 메시지를 지우고 상태를 회수한다")
    void openerAnalysis_Can부족_요청메시지를지우고상태를회수한다() {
        readyForOpenerAnalysis();
        when(canService.useUserCan(USER_ID, 1)).thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_CANS));

        service.openerAnalysis(openerRequest(), USER_ID);

        // 요청 메시지는 차감보다 먼저 저장된다 -> 지우지 않으면 답 없는 USER 메시지가 남아 다음 턴 문맥과 영구 저장에 섞인다
        verify(chatRedisService).removeLastMessage(eq(SESSION_ID), any(RedisMessageDto.class));
        verify(stateMachineService).sendEvent(SESSION_ID, ChatSessionEvent.STREAM_ERROR);
        verify(ragService, never()).generateSimilarProblemStream(any());
        verify(canService, never()).recoverUserCan(any(), anyInt());
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
