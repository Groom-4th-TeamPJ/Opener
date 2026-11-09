package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.reactivestreams.Subscription;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Sinks;
import spring.backend.domain.can.service.spec.CanService;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 종료 경로(버퍼 초과 · 연결 해제 · 클라이언트 취소)가 받은 만큼 저장하고 구독과 상태 머신을 정리하는지 검증
// LLM 응답은 테스트가 한 청크씩 밀어 넣는다 -> 청크 전달 시점과 취소 시점을 직접 정할 수 있다
@ExtendWith(MockitoExtension.class)
// 콜백이 boundedElastic 에서 돌아 검증 시점에 아직 안 불린 스텁이 생길 수 있어 lenient
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatServiceImplTerminationTest {

    private static final Long SESSION_ID = 1L;
    private static final UUID USER_ID = UUID.randomUUID();
    private static final long AWAIT_SECONDS = 5;

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

    private final Sinks.Many<String> llm = Sinks.many().unicast().onBackpressureBuffer();
    private final CountDownLatch upstreamCancelled = new CountDownLatch(1);
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
                new SimpleMeterRegistry()
        );

        when(userRepository.findUserById(USER_ID)).thenReturn(user);
        when(user.getName()).thenReturn("테스터");
        when(stateMachineService.sendEvent(SESSION_ID, ChatSessionEvent.SEND_MESSAGE)).thenReturn(true);
        // 세션 생성 시 examResultId 소유 검사를 통과시킨다
        when(examResultRepository.existsByIdAndUserId(SESSION_ID, USER_ID)).thenReturn(true);
        when(llmService.chatStream(SESSION_ID.toString(), "질문"))
                .thenReturn(llm.asFlux().doOnCancel(upstreamCancelled::countDown));
    }

    @Test
    @DisplayName("버퍼 초과로 끊기면 그때까지 받은 응답을 저장하고 상태 머신을 정리한다")
    void processMessage_버퍼초과_받은만큼저장하고상태머신을정리한다() {
        overflowTheSink();

        // 구독 취소된 스트림은 doOnComplete/doOnError 를 안 타서 이 턴의 응답이 통째로 사라졌다
        ArgumentCaptor<RedisMessageDto> saved = ArgumentCaptor.forClass(RedisMessageDto.class);
        verify(chatRedisService, timeout(2000).atLeast(2)).saveMessage(eq(SESSION_ID), saved.capture());
        RedisMessageDto partial = saved.getAllValues().stream()
                .filter(dto -> dto.chatRole() == ChatRole.LLM)
                .findFirst()
                .orElseThrow(() -> new AssertionError("LLM 부분 응답이 저장되지 않음"));
        assertTrue(partial.message().startsWith("c0c1c2"), "받은 앞부분이 저장돼야 함: " + partial.message());
        assertFalse(partial.message().contains("c299"), "버퍼 초과 뒤 도착한 청크까지 저장됨");
        // 표식이 없으면 대화 저장 소비자가 잘린 응답을 완결 답변으로 영구 저장한다
        assertTrue(partial.partial(), "부분 응답에 partial 표식이 없음");

        // 상태가 STREAMING 에 남으면 재연결 때 CONNECT_SUCCESS 가 거부된다
        verify(stateMachineService, timeout(2000)).removeMachine(SESSION_ID);
    }

    @Test
    @DisplayName("버퍼 초과로 끊기면 LLM 구독이 취소되어 이후 청크를 소비하지 않는다")
    void processMessage_버퍼초과_LLM구독이해제된다() throws Exception {
        overflowTheSink();

        assertTrue(upstreamCancelled.await(AWAIT_SECONDS, TimeUnit.SECONDS), "LLM 구독이 취소되지 않음");
        assertEquals(0, llm.currentSubscriberCount());
    }

    @Test
    @DisplayName("스트리밍 중 disconnect 하면 LLM 구독이 dispose 되고 이후 청크가 소비되지 않는다")
    void disconnectSession_스트리밍중_구독이dispose되고청크가멈춘다() throws Exception {
        CountDownLatch chunkSeen = new CountDownLatch(1);
        Disposable sse = startStreamingAndWaitForFirstChunk(chunkSeen);
        Disposable live = liveSubscription();
        assertFalse(live.isDisposed());

        service.disconnectSession(SESSION_ID, USER_ID);

        // dispose 하지 않으면 제거된 sink 로 토큰이 계속 흘러 OpenAI 비용이 이어진다
        assertTrue(live.isDisposed(), "disconnect 뒤에도 LLM 구독이 살아 있음");
        assertTrue(llm.tryEmitNext("늦은청크").isFailure(), "disconnect 뒤에도 청크가 소비됨");
        sse.dispose();
    }

    @Test
    @DisplayName("스트리밍 중 disconnect 하면 그때까지 받은 응답을 저장한다")
    void disconnectSession_스트리밍중_받은만큼저장한다() throws Exception {
        CountDownLatch chunkSeen = new CountDownLatch(1);
        Disposable sse = startStreamingAndWaitForFirstChunk(chunkSeen);

        service.disconnectSession(SESSION_ID, USER_ID);

        verify(chatRedisService, timeout(2000)).saveMessage(eq(SESSION_ID), savedLlmMessage("앞부분"));
        sse.dispose();
    }

    @Test
    @DisplayName("스트리밍 중 disconnect 해도 부분 응답이 든 메시지 목록과 세션 키를 지우지 않는다")
    void disconnectSession_스트리밍중_저장한부분응답을지우지않는다() throws Exception {
        CountDownLatch chunkSeen = new CountDownLatch(1);
        Disposable sse = startStreamingAndWaitForFirstChunk(chunkSeen);

        service.disconnectSession(SESSION_ID, USER_ID);

        verify(chatRedisService, timeout(2000)).saveMessage(eq(SESSION_ID), savedLlmMessage("앞부분"));
        // 소비자는 저장 이벤트가 도착한 뒤에 Redis 목록을 읽는다 -> 여기서 지우면 방금 저장한 부분 응답과
        // 소유자 검증용 세션 키가 함께 사라져 소비자가 빈 버퍼를 본다. 정리는 TTL 과 소비자의 deleteMessage 가 맡는다
        verify(chatRedisService, never()).deleteSession(any());
        verify(chatRedisService, never()).deleteMessage(any());
        sse.dispose();
    }

    @Test
    @DisplayName("클라이언트가 SSE 를 취소하면 구독 해제·상태 머신 제거·부분 응답 저장이 일어난다")
    void processMessage_클라이언트취소_구독해제와부분응답저장이유지된다() throws Exception {
        CountDownLatch chunkSeen = new CountDownLatch(1);
        Disposable sse = startStreamingAndWaitForFirstChunk(chunkSeen);

        sse.dispose();

        assertTrue(upstreamCancelled.await(AWAIT_SECONDS, TimeUnit.SECONDS), "LLM 구독이 취소되지 않음");
        verify(stateMachineService, timeout(2000)).removeMachine(SESSION_ID);
        verify(chatRedisService, timeout(2000)).saveMessage(eq(SESSION_ID), savedLlmMessage("앞부분"));
    }

    // 요청을 하나도 하지 않는 느린 클라이언트 -> sink 버퍼(256) 가 차면 FAIL_OVERFLOW, 청크 300개로 그 경로를 만든다
    // 구독자가 아예 없으면 FAIL_ZERO_SUBSCRIBER 라 버퍼 초과가 아니다
    private void overflowTheSink() {
        service.connectSession(SESSION_ID, USER_ID).subscribe(new BaseSubscriber<ServerSentEvent<String>>() {
            @Override
            protected void hookOnSubscribe(Subscription subscription) {
            }
        });
        service.processMessage(new ChatSendRequest(SESSION_ID, 10L, "질문"), USER_ID);
        for (int i = 0; i < 300; i++) {
            llm.tryEmitNext("c" + i);
        }
    }

    private Disposable startStreamingAndWaitForFirstChunk(CountDownLatch chunkSeen) throws Exception {
        Disposable sse = service.connectSession(SESSION_ID, USER_ID)
                .subscribe(event -> {
                    if ("message".equals(event.event())) {
                        chunkSeen.countDown();
                    }
                });
        service.processMessage(new ChatSendRequest(SESSION_ID, 10L, "질문"), USER_ID);
        llm.tryEmitNext("앞부분");
        assertTrue(chunkSeen.await(AWAIT_SECONDS, TimeUnit.SECONDS), "첫 청크가 SSE 로 나가지 않음");
        return sse;
    }

    // 구독 맵의 private 필드를 읽는다 -> dispose 여부를 밖에서 볼 방법이 이것뿐이다
    @SuppressWarnings("unchecked")
    private Disposable liveSubscription() {
        var subscriptions = (ConcurrentHashMap<Long, Disposable>) ReflectionTestUtils.getField(service, "subscriptions");
        Disposable live = subscriptions.get(SESSION_ID);
        assertNotNull(live, "진행 중인 LLM 구독이 등록되지 않음");
        return live;
    }

    private RedisMessageDto savedLlmMessage(String text) {
        return org.mockito.ArgumentMatchers.argThat(
                dto -> dto != null && dto.chatRole() == ChatRole.LLM && text.equals(dto.message()));
    }
}
