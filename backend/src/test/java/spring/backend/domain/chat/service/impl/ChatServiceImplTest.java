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
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mockito.ArgumentCaptor;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
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

        // 세션 생성 시 examResultId 소유 검사를 통과시킨다
        when(examResultRepository.existsByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(true);
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

    @Test
    @DisplayName("조립 단계에서 동기로 던져도 error 이벤트와 STREAM_ERROR 로 상태를 회수한다")
    void processMessage_조립단계예외_상태를회수한다() {
        // 조립 시점 예외는 doOnError 를 타지 않는다
        // 상태를 되돌리지 않으면 그 세션은 재연결 전까지 모든 메시지가 SESSION_EXPIRED 로 거부된다
        when(llmService.chatStream(SESSION_ID.toString(), "질문"))
                .thenThrow(new IllegalStateException("벡터 검색 실패"));

        StepVerifier.create(service.connectSession(SESSION_ID, USER_ID))
                .assertNext(sse -> assertEquals("connected", sse.event()))
                .then(() -> assertThrows(IllegalStateException.class,
                        () -> service.processMessage(request(), USER_ID)))
                .assertNext(sse -> {
                    assertEquals("error", sse.event());
                    assertEquals(ErrorCode.LLM_RESPONSE_FAIL.getCode(), field(sse, "errorCode"));
                })
                .thenCancel()
                .verify(TIMEOUT);

        verify(stateMachineService).sendEvent(SESSION_ID, ChatSessionEvent.STREAM_ERROR);
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

    @Test
    @DisplayName("스트리밍 도중 끊기면 이미 받은 부분 응답을 저장한다")
    void processMessage_스트리밍중단_부분응답이저장된다() {
        when(llmService.chatStream(SESSION_ID.toString(), "질문"))
                .thenReturn(Flux.concat(
                        Flux.just("앞부분"),
                        Flux.error(new IllegalStateException("stream broke"))));

        StepVerifier.create(service.connectSession(SESSION_ID, USER_ID))
                .assertNext(sse -> assertEquals("connected", sse.event()))
                .then(() -> service.processMessage(request(), USER_ID))
                .assertNext(sse -> assertEquals("message", sse.event()))
                .assertNext(sse -> assertEquals("error", sse.event()))
                .thenCancel()
                .verify(TIMEOUT);

        // 화면에는 절반 쓰인 답변이 남는데 히스토리에 없으면 다음 턴에 모델과 사용자가 다른 대화를 본다
        ArgumentCaptor<RedisMessageDto> captor = ArgumentCaptor.forClass(RedisMessageDto.class);
        verify(chatRedisService, timeout(2000).atLeastOnce())
                .saveMessage(eq(SESSION_ID), captor.capture());

        boolean partialSaved = captor.getAllValues().stream()
                .anyMatch(dto -> "앞부분".equals(dto.message()));
        assertTrue(partialSaved);
    }

    @Test
    @DisplayName("연결이 해제되면 LLM 구독도 취소한다")
    void disconnectSession_연결해제_LLM구독이취소된다() {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        when(llmService.chatStream(SESSION_ID.toString(), "질문"))
                .thenReturn(Flux.<String>never().doOnCancel(() -> cancelled.set(true)));

        StepVerifier.create(service.connectSession(SESSION_ID, USER_ID))
                .assertNext(sse -> assertEquals("connected", sse.event()))
                .then(() -> service.processMessage(request(), USER_ID))
                .then(() -> service.disconnectSession(SESSION_ID, USER_ID))
                .thenCancel()
                .verify(TIMEOUT);

        // 구독을 정리하지 않으면 클라이언트가 끊어도 OpenAI 스트림은 끝까지 돌며 토큰을 소비한다
        assertTrue(cancelled.get());
    }

    @Test
    @DisplayName("청크 콜백은 응답을 읽는 스레드가 아니라 boundedElastic 워커에서 실행된다")
    void processMessage_콜백이_응답수신스레드밖에서_실행된다() throws Exception {
        // 응답을 읽는 레인을 이름 붙인 스케줄러로 흉내낸다
        // "소스가 어디서 emit 하든 publishOn 아래는 boundedElastic" 이 주장이라
        // 소스는 OpenAI 가 아니어도 되고, 이름만 구별되면 경계 앞뒤를 갈라 볼 수 있다
        Scheduler fakeResponseLoop = Schedulers.newSingle("fake-llm-loop");
        AtomicReference<String> sourceThread = new AtomicReference<>();
        AtomicReference<String> callbackThread = new AtomicReference<>();
        CountDownLatch callbackRan = new CountDownLatch(1);

        try {
            when(llmService.chatStream(SESSION_ID.toString(), "질문"))
                    .thenReturn(Flux.just("토큰1", "토큰2")
                            .subscribeOn(fakeResponseLoop)
                            // publishOn 앞이라 여기는 소스 스레드
                            .doOnNext(c -> sourceThread.set(Thread.currentThread().getName())));

            // STREAM_START 전이는 publishOn 아래 doOnNext 안에서 불린다 -> 콜백이 실제로 도는 스레드
            when(stateMachineService.sendEvent(SESSION_ID, ChatSessionEvent.STREAM_START))
                    .thenAnswer(inv -> {
                        callbackThread.set(Thread.currentThread().getName());
                        callbackRan.countDown();
                        return true;
                    });

            Disposable sse = service.connectSession(SESSION_ID, USER_ID).subscribe();
            service.processMessage(request(), USER_ID);
            assertTrue(callbackRan.await(5, TimeUnit.SECONDS), "콜백이 실행되지 않음");
            sse.dispose();
        } finally {
            fakeResponseLoop.dispose();
        }

        System.out.println("[스레드 경계] 소스=" + sourceThread.get() + " 콜백=" + callbackThread.get());

        assertTrue(sourceThread.get().startsWith("fake-llm-loop"),
                "소스가 흉내낸 응답 수신 레인에서 emit 하지 않음: " + sourceThread.get());
        // publishOn 을 빼면 이 검사가 깨진다 - 콜백이 소스 스레드에서 그대로 돈다
        // 같은 JVM 에서 spring.threads.virtual.enabled 컨텍스트가 먼저 뜨면 Reactor 가 loomBoundedElastic 이름을 쓴다
        assertTrue(callbackThread.get().contains("oundedElastic"),
                "콜백이 boundedElastic 으로 넘어가지 않음: " + callbackThread.get());
        assertNotEquals(sourceThread.get(), callbackThread.get(),
                "콜백이 응답 수신 스레드에서 그대로 실행됨 - 오프로딩 미적용");
    }

    @Test
    @DisplayName("한 세션의 콜백 블로킹이 같은 루프를 쓰는 다른 세션의 첫 청크를 밀어내지 않는다")
    void processMessage_한세션의_블로킹이_다른세션을_밀어내지_않는다() throws Exception {
        final long BLOCK_MS = 300;                 // A 콜백 안의 Redis 저장 · 상태 전이를 대신하는 지연
        final Long OTHER = 2L;
        Scheduler sharedLoop = Schedulers.newSingle("shared-loop");   // 두 세션이 한 루프를 공유
        CountDownLatch bServed = new CountDownLatch(1);
        AtomicLong bServedAt = new AtomicLong();
        long startedAt;

        try {
            when(llmService.chatStream(SESSION_ID.toString(), "질문"))
                    .thenReturn(Flux.just("A").subscribeOn(sharedLoop));
            when(llmService.chatStream(OTHER.toString(), "질문"))
                    .thenReturn(Flux.just("B").subscribeOn(sharedLoop));

            when(stateMachineService.sendEvent(OTHER, ChatSessionEvent.SEND_MESSAGE)).thenReturn(true);
            when(examResultRepository.existsByIdAndUserId(OTHER, USER_ID)).thenReturn(true);
            when(stateMachineService.sendEvent(SESSION_ID, ChatSessionEvent.STREAM_START))
                    .thenAnswer(inv -> { Thread.sleep(BLOCK_MS); return true; });
            when(stateMachineService.sendEvent(OTHER, ChatSessionEvent.STREAM_START))
                    .thenAnswer(inv -> { bServedAt.set(System.nanoTime()); bServed.countDown(); return true; });

            Disposable sseA = service.connectSession(SESSION_ID, USER_ID).subscribe();
            Disposable sseB = service.connectSession(OTHER, USER_ID).subscribe();

            startedAt = System.nanoTime();
            service.processMessage(new ChatSendRequest(SESSION_ID, 10L, "질문"), USER_ID);
            service.processMessage(new ChatSendRequest(OTHER, 10L, "질문"), USER_ID);

            assertTrue(bServed.await(5, TimeUnit.SECONDS), "B 세션 콜백이 실행되지 않음");
            sseA.dispose();
            sseB.dispose();
        } finally {
            sharedLoop.dispose();
        }

        long delayMs = (bServedAt.get() - startedAt) / 1_000_000;
        System.out.println("[오프로딩 효과] A 콜백 블로킹 " + BLOCK_MS + "ms · B 세션 첫 청크까지 " + delayMs + "ms");

        // publishOn 을 빼면 B 의 구독이 A 콜백 뒤에 줄을 서서 BLOCK_MS 만큼 밀린다
        assertTrue(delayMs < BLOCK_MS / 2,
                "B 가 A 의 블로킹만큼 밀렸다 - 오프로딩 미적용: " + delayMs + "ms");
    }

}
