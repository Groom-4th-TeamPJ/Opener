package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.transaction.PlatformTransactionManager;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import spring.backend.domain.can.service.spec.CanService;
import spring.backend.domain.chat.dto.request.ChatSendRequest;
import spring.backend.domain.chat.mapper.RedisMessageMapper;
import spring.backend.domain.chat.messaging.ChatMessageProducer;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.domain.chat.service.spec.RagService;
import spring.backend.domain.chat.statemachine.ChatSessionEvent;
import spring.backend.domain.chat.statemachine.ChatSessionStateMachineService;
import spring.backend.domain.exam.repository.jpa.JpaQuestionRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.domain.user.model.entity.User;
import spring.backend.domain.user.repository.spec.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

// LLM 은 전부 mock -> OpenAI 실호출 없이 스트리밍 계약(청크 순서·상태 전이·에러 회수)만 검증
@ExtendWith(MockitoExtension.class)
// 콜백이 boundedElastic 에서 돌아 검증 시점에 아직 안 불린 스텁이 생길 수 있어 lenient
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceImplTest {

    private static final Long SESSION_ID = 1L;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

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
    @Mock private User user;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        // MeterRegistry 는 생성자에서 Gauge 를 등록하므로 mock 대신 실제 구현 사용
        service = new ChatServiceImpl(
                chatRedisService,
                llmService,
                ragService,
                objectMapper,
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
                new SimpleMeterRegistry()
        );

        // connectSession 이 통과하기 위한 최소 스텁
        when(userRepository.findUserById(USER_ID)).thenReturn(user);
        when(user.getName()).thenReturn("테스터");

        // SEND_MESSAGE 전이가 거부되면 processMessage 가 SESSION_EXPIRED 로 먼저 끊김
        when(stateMachineService.sendEvent(SESSION_ID, ChatSessionEvent.SEND_MESSAGE)).thenReturn(true);
    }

    @Test
    @DisplayName("청크가 순서대로 SSE 로 전달되고 STREAM_START/STREAM_COMPLETE 가 발화된다")
    void processMessage_streamsChunksInOrder() {
        when(llmService.chatStream(SESSION_ID.toString(), "질문"))
                .thenReturn(Flux.just("토큰1", "토큰2"));

        StepVerifier.create(service.connectSession(SESSION_ID, USER_ID))
                .assertNext(sse -> assertEquals("connected", sse.event()))
                .then(() -> service.processMessage(request(), USER_ID))
                .assertNext(sse -> {
                    assertEquals("message", sse.event());
                    assertEquals("토큰1", field(sse, "chunk"));
                })
                .assertNext(sse -> {
                    assertEquals("message", sse.event());
                    assertEquals("토큰2", field(sse, "chunk"));
                })
                .assertNext(sse -> assertEquals("complete", sse.event()))
                .thenCancel()
                .verify(TIMEOUT);

        // 첫 청크에서 1회만 STREAM_START -> compareAndSet 보장 확인
        verify(stateMachineService, timeout(2000)).sendEvent(SESSION_ID, ChatSessionEvent.STREAM_START);
        verify(stateMachineService, timeout(2000)).sendEvent(SESSION_ID, ChatSessionEvent.STREAM_COMPLETE);
        verify(stateMachineService, never()).sendEvent(SESSION_ID, ChatSessionEvent.STREAM_ERROR);
    }

    @Test
    @DisplayName("빈 스트림이면 STREAM_COMPLETE 가 아니라 STREAM_ERROR 로 상태를 회수한다")
    void processMessage_emptyStream_recoversWithStreamError() {
        when(llmService.chatStream(SESSION_ID.toString(), "질문"))
                .thenReturn(Flux.empty());

        StepVerifier.create(service.connectSession(SESSION_ID, USER_ID))
                .assertNext(sse -> assertEquals("connected", sse.event()))
                .then(() -> service.processMessage(request(), USER_ID))
                .assertNext(sse -> {
                    assertEquals("error", sse.event());
                    assertEquals("C_006", field(sse, "errorCode"));
                })
                .thenCancel()
                .verify(TIMEOUT);

        // STREAM_COMPLETE 를 보내면 거부되어 PROCESSING 고착 -> 다음 요청이 SESSION_EXPIRED 로 막힘
        verify(stateMachineService, timeout(2000)).sendEvent(SESSION_ID, ChatSessionEvent.STREAM_ERROR);
        verify(stateMachineService, never()).sendEvent(SESSION_ID, ChatSessionEvent.STREAM_COMPLETE);
        verify(stateMachineService, never()).sendEvent(SESSION_ID, ChatSessionEvent.STREAM_START);
    }

    @Test
    @DisplayName("스트림이 에러로 끝나면 error 이벤트를 내보내고 STREAM_ERROR 로 전이한다")
    void processMessage_errorStream_emitsError() {
        when(llmService.chatStream(SESSION_ID.toString(), "질문"))
                .thenReturn(Flux.error(new RuntimeException("LLM 다운")));

        StepVerifier.create(service.connectSession(SESSION_ID, USER_ID))
                .assertNext(sse -> assertEquals("connected", sse.event()))
                .then(() -> service.processMessage(request(), USER_ID))
                .assertNext(sse -> {
                    assertEquals("error", sse.event());
                    // 내부 예외 문구("LLM 다운")를 그대로 노출하지 않는다 - 사용자에게 의미가 없고 내부 구조가 샌다
                    assertEquals(ErrorCode.LLM_RESPONSE_FAIL.getMessage(), field(sse, "error"));
                    assertEquals(ErrorCode.LLM_RESPONSE_FAIL.getCode(), field(sse, "errorCode"));
                })
                .thenCancel()
                .verify(TIMEOUT);

        verify(stateMachineService, timeout(2000)).sendEvent(SESSION_ID, ChatSessionEvent.STREAM_ERROR);
        verify(stateMachineService, never()).sendEvent(SESSION_ID, ChatSessionEvent.STREAM_COMPLETE);
    }

    @Test
    @DisplayName("서킷 OPEN 으로 실패하면 SSE error 이벤트에 C_010 이 실린다")
    void processMessage_서킷오픈_에러코드가전달된다() {
        when(llmService.chatStream(SESSION_ID.toString(), "질문"))
                .thenReturn(Flux.error(new BusinessException(ErrorCode.LLM_CIRCUIT_OPEN)));

        StepVerifier.create(service.connectSession(SESSION_ID, USER_ID))
                .assertNext(sse -> assertEquals("connected", sse.event()))
                .then(() -> service.processMessage(request(), USER_ID))
                .assertNext(sse -> {
                    assertEquals("error", sse.event());
                    // 서킷 OPEN 과 일반 LLM 실패를 클라이언트가 구분해야 재시도 안내가 성립한다
                    assertEquals("C_010", field(sse, "errorCode"));
                })
                .thenCancel()
                .verify(TIMEOUT);
    }

    private ChatSendRequest request() {
        return new ChatSendRequest(SESSION_ID, 10L, "질문");
    }

    // SSE data 는 SseMessageResponse 직렬화 JSON -> 필드 하나만 꺼내 비교
    private String field(ServerSentEvent<String> sse, String name) {
        try {
            var node = objectMapper.readTree(sse.data()).get(name);
            return node == null ? null : node.asText();
        } catch (Exception e) {
            throw new IllegalStateException("SSE 페이로드 파싱 실패: " + sse.data(), e);
        }
    }
}
