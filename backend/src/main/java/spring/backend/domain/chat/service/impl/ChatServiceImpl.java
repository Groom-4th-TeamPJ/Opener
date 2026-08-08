package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import spring.backend.domain.can.service.spec.CanService;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
import spring.backend.domain.chat.dto.request.ChatSaveRequest;
import spring.backend.domain.chat.dto.request.ChatSendRequest;
import spring.backend.domain.chat.dto.request.OpenerAnalysisRequest;
import spring.backend.domain.chat.dto.response.ChatHistoryResponse;
import spring.backend.domain.chat.dto.response.ChatMessageDto;
import spring.backend.domain.chat.dto.response.SseMessageResponse;
import spring.backend.domain.chat.mapper.RedisMessageMapper;
import spring.backend.domain.chat.messaging.ChatMessageProducer;
import spring.backend.domain.chat.model.entity.ChatMessage;
import spring.backend.domain.chat.model.entity.ChatMessageContent;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.domain.chat.service.spec.ChatService;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.domain.chat.service.spec.RagService;
import spring.backend.domain.chat.statemachine.ChatSessionEvent;
import spring.backend.domain.chat.statemachine.ChatSessionStateMachineService;
import spring.backend.domain.exam.model.dto.Passage;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.exam.repository.jpa.JpaQuestionRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.domain.user.model.entity.User;
import spring.backend.domain.user.repository.spec.UserRepository;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

// 인터페이스 구현체에만 @Service -> spec 은 추상, 구현 교체/테스트 대역 주입이 쉬워짐
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    // 모든 의존성 final + 생성자 주입 -> 불변 보장, 외부에서 교체 불가하여 안전하게 사용
    private final ChatRedisService chatRedisService;
    private final LlmService llmService;
    private final RagService ragService;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final RedisMessageMapper redisMessageMapper;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageProducer chatMessageProducer;
    private final StringRedisTemplate redisTemplate;
    private final JpaQuestionRepository questionRepository;
    private final QuestionResultRepository questionResultRepository;
    private final ExamResultRepository examResultRepository;
    private final CanService canService;
    private final ChatSessionStateMachineService stateMachineService;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;

    // 활성 SSE 연결을 메모리에 보관 -> 메시지 도착 시 해당 세션 Sink 로 즉시 푸시 가능
    // ConcurrentHashMap -> 동시 연결/해제가 같은 Map 을 건드려도 락 없이 스레드 안전
    private final ConcurrentHashMap<Long, Sinks.Many<ServerSentEvent<String>>> sinks = new ConcurrentHashMap<>();

    public ChatServiceImpl(
            ChatRedisService chatRedisService,
            LlmService llmService,
            RagService ragService,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            RedisMessageMapper redisMessageMapper,
            ChatMessageRepository chatMessageRepository,
            ChatMessageProducer chatMessageProducer,
            // @Qualifier -> Redis 빈이 둘이므로 채팅 클러스터 템플릿을 명시 선택
            @Qualifier("chatRedisTemplate") StringRedisTemplate redisTemplate,
            JpaQuestionRepository questionRepository,
            QuestionResultRepository questionResultRepository,
            ExamResultRepository examResultRepository,
            CanService canService,
            ChatSessionStateMachineService stateMachineService,
            // 매니저만 주입받아 직접 TransactionTemplate 구성 -> 리액티브 콜백 안에서 트랜잭션 범위를 수동 제어
            PlatformTransactionManager transactionManager,
            // 모니터링용 -> 활성 SSE 연결 수를 Prometheus 게이지로 노출
            MeterRegistry meterRegistry
    ) {
        this.chatRedisService = chatRedisService;
        this.llmService = llmService;
        this.ragService = ragService;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.redisMessageMapper = redisMessageMapper;
        this.chatMessageRepository = chatMessageRepository;
        this.chatMessageProducer = chatMessageProducer;
        this.redisTemplate = redisTemplate;
        this.questionRepository = questionRepository;
        this.questionResultRepository = questionResultRepository;
        this.examResultRepository = examResultRepository;
        this.canService = canService;
        this.stateMachineService = stateMachineService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.meterRegistry = meterRegistry;

        // 활성 SSE 연결 수 게이지 등록 -> 헤드라인 지표(동시 연결)를 Grafana 에서 실시간 관측
        Gauge.builder("chat.sse.active.connections", sinks, java.util.Map::size)
                .description("현재 활성 SSE 연결 수")
                .register(meterRegistry);
    }

    @Override
    public Flux<ServerSentEvent<String>> connectSession(Long sessionId, UUID userId) {

        log.info("[SSE] 연결 요청 시작 - sessionId: {}, userId: {}", sessionId, userId);

        // 기존 Sink 재사용 -> 네트워크 끊김 후 재연결 시 진행 중이던 대화 스트림을 잃지 않게 함
        Sinks.Many<ServerSentEvent<String>> existingSink = sinks.get(sessionId);
        if (existingSink != null) {
            log.info("[SSE] 기존 연결 재사용 - sessionId: {}, userId: {}, 현재 활성 연결 수: {}",
                    sessionId, userId, sinks.size());

            chatRedisService.validateSessionOwner(sessionId, userId);
            return existingSink.asFlux();
        }

        // 새 SSE 연결 생성
        try {
            log.info("[SSE] 새로운 연결 생성 시작 - sessionId: {}, userId: {}", sessionId, userId);

            User user = userRepository.findUserById(userId);
            if (user == null) {
                log.error("[SSE] 사용자를 찾을 수 없음 - userId: {}", userId);
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }

            log.debug("[SSE] 사용자 조회 완료 - userId: {}, userName: {}", userId, user.getName());

            // 세션용 레디스 초기화
            chatRedisService.initializeSession(sessionId, userId);

            // State Machine: 연결 성공 상태 전이 (IDLE → CONNECTED)
            stateMachineService.getOrCreateMachine(sessionId);
            if (!stateMachineService.sendEvent(sessionId, ChatSessionEvent.CONNECT_SUCCESS)) {
                log.warn("[SSE] CONNECT_SUCCESS 전이 거부 - sessionId: {}, 현재 상태: {}",
                        sessionId, stateMachineService.getCurrentState(sessionId));
            }

            // multicast -> 같은 세션 다중 구독 허용, onBackpressureBuffer -> 클라이언트가 느려도 청크 버퍼링해 유실 방지
            Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();

            // 인프라(Redis·StateMachine) 준비 완료 후 마지막에 등록 -> 절반만 준비된 세션에 메시지 유입 방지
            sinks.put(sessionId, sink);

            log.debug("[SSE] Sink 저장 완료 - sessionId: {}", sessionId);

            // 클라이언트에게 연결 성공 알림
            emitConnected(sessionId);

            log.info("[SSE] 새 연결 성공 - sessionId: {}, userId: {}, userName: {}, 현재 활성 연결 수: {}",
                    sessionId, userId, user.getName(), sinks.size());

            // doOnCancel 로 정리 -> 클라이언트가 끊으면 Sink/StateMachine 을 비워 메모리 누수 방지
            return sink.asFlux()
                    .doOnCancel(() -> {
                        sinks.remove(sessionId);
                        stateMachineService.removeMachine(sessionId);
                        log.info("[SSE] 클라이언트 연결 해제 (cancel) - sessionId: {}, 남은 연결 수: {}",
                                sessionId, sinks.size());
                    });

        // BusinessException 은 그대로 통과 -> 소유자 불일치(INVALID_SESSION)가 SESSION_INITIALIZE_FAIL 로 뭉개지면
        // 클라이언트도 로그도 인증 거부인지 인프라 장애인지 구분할 수 없음
        // 정리(remove) 도 하지 않음 -> 거부된 연결이 정상 소유자의 Sink/StateMachine 을 지우면 그 자체가 공격 수단
        } catch (BusinessException e) {
            log.warn("[SSE] 연결 거부 - sessionId: {}, userId: {}, errorCode: {}",
                    sessionId, userId, e.getErrorCode().getCode());
            throw e;
        } catch (Exception e) {
            sinks.remove(sessionId);
            stateMachineService.removeMachine(sessionId);
            log.error("[SSE] 연결 생성 실패 - sessionId: {}, userId: {}, 오류: {}",
                    sessionId, userId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SESSION_INITIALIZE_FAIL);
        }
    }

    // @Transactional -> 세션 종료에 동반되는 DB 변경을 원자적으로 묶음
    @Override
    @Transactional
    public void disconnectSession(Long sessionId, UUID userId) {

        log.info("[SSE] 세션 해제 요청 - sessionId: {}, userId: {}", sessionId, userId);

        Sinks.Many<ServerSentEvent<String>> sink = sinks.get(sessionId);

        if (sink != null) {
            chatRedisService.validateSessionOwner(sessionId, userId);

            log.debug("[SSE] 세션 권한 검증 완료 - sessionId: {}", sessionId);

            // Sink 완료 → 구독 중인 Flux가 onComplete 수신 → SSE 연결 종료
            sink.tryEmitComplete();
            sinks.remove(sessionId);

            chatRedisService.deleteSession(sessionId);

            // State Machine: 세션 종료 (→ IDLE) + 제거
            if (!stateMachineService.sendEvent(sessionId, ChatSessionEvent.CLOSE)) {
                log.warn("[SSE] CLOSE 전이 거부 - sessionId: {}, 현재 상태: {}",
                        sessionId, stateMachineService.getCurrentState(sessionId));
            }
            stateMachineService.removeMachine(sessionId);

            log.info("[SSE] 세션 해제 완료 - sessionId: {}, userId: {}, 남은 연결 수: {}",
                    sessionId, userId, sinks.size());
        } else {
            log.warn("[SSE] 세션 해제 실패 - 존재하지 않는 세션 - sessionId: {}, userId: {}, 활성 세션 목록: {}",
                    sessionId, userId, sinks.keySet());
            throw new BusinessException(ErrorCode.INVALID_SESSION);
        }
    }

    @Override
    public void processMessage(ChatSendRequest req, UUID userId) {

        Long sessionId = req.sessionId();
        String userMessage = req.message();

        log.info("[Chat] 메시지 처리 시작 - sessionId: {}, userId: {}, 현재 활성 연결 수: {}",
                sessionId, userId, sinks.size());

        // Sink 존재 확인 (SSE 연결 유지 여부)
        if (!sinks.containsKey(sessionId)) {
            log.error("[Chat] SSE 연결 없음 - sessionId: {}, 활성 세션 목록: {}",
                    sessionId, sinks.keySet());
            throw new BusinessException(ErrorCode.SESSION_EXPIRED);
        }

        log.debug("[Chat] SSE 연결 확인 완료 - sessionId: {}", sessionId);

        // 권한 검증
        chatRedisService.validateSessionOwner(sessionId, userId);

        // State Machine: SEND_MESSAGE (CONNECTED/COMPLETED → PROCESSING)
        if (!stateMachineService.sendEvent(sessionId, ChatSessionEvent.SEND_MESSAGE)) {
            log.warn("[Chat] 상태 전이 거부 - sessionId: {}, 현재 상태: {}",
                    sessionId, stateMachineService.getCurrentState(sessionId));
            throw new BusinessException(ErrorCode.SESSION_EXPIRED);
        }

        // 사용자 메시지를 Redis에 저장
        RedisMessageDto userRedisMessageDto = redisMessageMapper.toDtoUser(req);
        chatRedisService.saveMessage(sessionId, userRedisMessageDto);

        // LLM 응답 수집용
        StringBuilder llmResponseBuilder = new StringBuilder();
        AtomicBoolean streamStarted = new AtomicBoolean(false);
        long ttftStartNanos = System.nanoTime();   // 첫 토큰 지연(TTFT) 측정 시작점

        // non-blocking subscribe -> 요청 스레드를 붙잡지 않고 청크가 올 때마다 콜백 실행, 동시성 확보
        llmService.chatStream(sessionId.toString(), userMessage)
                // publishOn -> 이후 콜백을 boundedElastic 으로 옮김
                // 콜백 안에 Redis 저장·blockLast 상태 전이 같은 블로킹이 있어 OpenAI 응답을 읽는 이벤트 루프에서 실행되면
                // 그 루프가 담당하는 다른 커넥션까지 함께 멈춤
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(chunk -> {
                    // compareAndSet -> 첫 청크에서만 STREAM_START 를 한 번 보내도록 원자적 보장
                    if (streamStarted.compareAndSet(false, true)) {
                        // 첫 청크에서만 스레드명 기록 -> 오프로딩이 실제로 걸렸는지 확인할 실측 근거
                        // 매 청크마다 찍으면 로그가 토큰 수만큼 늘어남
                        log.debug("[Chat] 스트림 콜백 스레드 - sessionId: {}, thread: {}",
                                sessionId, Thread.currentThread().getName());

                        if (!stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_START)) {
                            log.warn("[Chat] STREAM_START 전이 거부 - sessionId: {}, 현재 상태: {}",
                                    sessionId, stateMachineService.getCurrentState(sessionId));
                        }
                        // 첫 토큰까지 시간(TTFT) 계측 -> 대시보드 첫 토큰 지연 패널
                        meterRegistry.timer("chat.sse.ttft")
                                .record(System.nanoTime() - ttftStartNanos, java.util.concurrent.TimeUnit.NANOSECONDS);
                    }

                    // SSE 청크 전송
                    emitChunk(sessionId, chunk);

                    // 전체 응답 수집
                    llmResponseBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    // 청크가 0개면 STREAM_START 가 발화되지 않아 상태는 아직 PROCESSING
                    // 여기서 STREAM_COMPLETE 를 보내면 거부되고 세션이 PROCESSING 에 고착 -> 다음 요청이 SESSION_EXPIRED 로 막힘
                    if (!streamStarted.get()) {
                        log.warn("[Chat] 빈 응답 스트림 - sessionId: {}, 상태 회수를 위해 STREAM_ERROR 전이", sessionId);
                        emitError(sessionId,
                                ErrorCode.LLM_RESPONSE_FAIL.getMessage(),
                                ErrorCode.LLM_RESPONSE_FAIL.getCode());
                        if (!stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_ERROR)) {
                            log.warn("[Chat] STREAM_ERROR 전이 거부 - sessionId: {}, 현재 상태: {}",
                                    sessionId, stateMachineService.getCurrentState(sessionId));
                        }
                        return;
                    }

                    // 완전한 LLM 응답을 Redis에 저장
                    String fullLlmResponse = llmResponseBuilder.toString();
                    RedisMessageDto llmRedisMessageDto = redisMessageMapper.toDtoLlm(fullLlmResponse);
                    chatRedisService.saveMessage(sessionId, llmRedisMessageDto);

                    // SSE 완료 이벤트
                    emitComplete(sessionId);

                    // State Machine: STREAM_COMPLETE (STREAMING → COMPLETED)
                    if (!stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_COMPLETE)) {
                        log.warn("[Chat] STREAM_COMPLETE 전이 거부 - sessionId: {}, 현재 상태: {}",
                                sessionId, stateMachineService.getCurrentState(sessionId));
                    }
                })
                .doOnError(error -> {
                    log.error("[Chat] 메시지 처리 중 예외 발생 - sessionId: {}", sessionId, error);
                    // 서킷 OPEN 과 LLM 실패를 클라이언트가 구분할 수 있어야 재시도 안내가 성립한다
                    // error.getMessage() 는 내부 예외 문구라 사용자에게 의미가 없고 코드도 실리지 않는다
                    if (error instanceof BusinessException be) {
                        emitError(sessionId, be.getErrorCode().getMessage(), be.getErrorCode().getCode());
                    } else {
                        emitError(sessionId,
                                ErrorCode.LLM_RESPONSE_FAIL.getMessage(),
                                ErrorCode.LLM_RESPONSE_FAIL.getCode());
                    }
                    if (!stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_ERROR)) {
                        log.warn("[Chat] STREAM_ERROR 전이 거부 - sessionId: {}, 현재 상태: {}",
                                sessionId, stateMachineService.getCurrentState(sessionId));
                    }
                })
                .subscribe();
    }

    @Override
    public void saveMessages(ChatSaveRequest req, UUID userId) {

        Long sessionId = req.sessionId();
        Long questionResultId = req.questionResultId();

        // SSE 연결 유지 확인
        if (!sinks.containsKey(sessionId)) {
            throw new BusinessException(ErrorCode.SESSION_EXPIRED);
        }

        // 권한 검증
        chatRedisService.validateSessionOwner(sessionId, userId);

        // 직접 저장 대신 이벤트 발행 -> DB 저장을 비동기로 넘겨 사용자 응답을 지연 없이 반환
        chatMessageProducer.publishSaveMessageEvent(sessionId, userId, questionResultId);

        log.info("[Chat] 대화 저장 이벤트 발행 완료 - sessionId: {}", sessionId);
    }

    @Override
    public void openerAnalysis(OpenerAnalysisRequest req, UUID userId) {

        Long sessionId = req.sessionId();
        Long questionId = req.questionId();
        Long questionResultId = req.questionResultId();

        // Sink 존재 확인
        if (!sinks.containsKey(sessionId)) {
            throw new BusinessException(ErrorCode.SESSION_EXPIRED);
        }

        // 세션 검증
        chatRedisService.validateSessionOwner(sessionId, userId);

        // State Machine: SEND_MESSAGE (CONNECTED/COMPLETED → PROCESSING)
        if (!stateMachineService.sendEvent(sessionId, ChatSessionEvent.SEND_MESSAGE)) {
            log.warn("[OpenerAnalysis] 상태 전이 거부 - sessionId: {}, 현재 상태: {}",
                    sessionId, stateMachineService.getCurrentState(sessionId));
            throw new BusinessException(ErrorCode.SESSION_EXPIRED);
        }

        // 스트리밍 시작 전 Can 선차감 -> 비용 검증 실패를 빨리 끊어 무의미한 LLM 호출 방지
        try {
            canService.useUserCan(userId, 1);
        } catch (BusinessException e) {
            ErrorCode errorCode = e.getErrorCode();
            log.warn("[OpenerAnalysis] Can 차감 실패 - userId: {}, errorCode: {}", userId, errorCode.getCode());
            emitError(sessionId, errorCode.getMessage(), errorCode.getCode());
            return;
        }

        // 사용자의 오프너 분석 요청 메시지를 Redis에 저장
        RedisMessageDto userRequestMessage = RedisMessageDto.builder()
                .chatRole(ChatRole.USER)
                .message("오프너 분석을 요청했습니다")
                .timestamp(java.time.LocalDateTime.now())
                .build();
        chatRedisService.saveMessage(sessionId, userRequestMessage);

        // Question 조회
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        String problemContext = buildProblemContext(question);

        // RAG 응답 수집용
        StringBuilder ragResponseBuilder = new StringBuilder();
        AtomicBoolean ragStreamStarted = new AtomicBoolean(false);

        // RAG Flux를 non-blocking subscribe
        ragService.generateSimilarProblemStream(problemContext)
                // publishOn -> 이후 콜백을 boundedElastic 으로 옮김
                // 여기 doOnComplete 는 JDBC 트랜잭션까지 돌리므로 이벤트 루프에서 실행되면 영향이 가장 큼
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(chunk -> {
                    // 첫 청크 → STREAM_START 상태 전이
                    if (ragStreamStarted.compareAndSet(false, true)) {
                        // 첫 청크에서만 스레드명 기록 -> 오프로딩 실측 근거
                        log.debug("[OpenerAnalysis] 스트림 콜백 스레드 - sessionId: {}, thread: {}",
                                sessionId, Thread.currentThread().getName());

                        if (!stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_START)) {
                            log.warn("[OpenerAnalysis] STREAM_START 전이 거부 - sessionId: {}, 현재 상태: {}",
                                    sessionId, stateMachineService.getCurrentState(sessionId));
                        }
                    }

                    // SSE 청크 전송
                    emitChunk(sessionId, chunk);

                    // 전체 응답 수집
                    ragResponseBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    // 청크가 0개면 STREAM_START 미발화 -> STREAM_COMPLETE 가 거부되어 PROCESSING 고착
                    // 결과물이 없으므로 markOpener 도 남기지 않고 선차감한 Can 을 되돌림
                    if (!ragStreamStarted.get()) {
                        log.warn("[OpenerAnalysis] 빈 응답 스트림 - sessionId: {}, 상태 회수 + Can 복구", sessionId);
                        emitError(sessionId,
                                ErrorCode.LLM_RESPONSE_FAIL.getMessage(),
                                ErrorCode.LLM_RESPONSE_FAIL.getCode());
                        if (!stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_ERROR)) {
                            log.warn("[OpenerAnalysis] STREAM_ERROR 전이 거부 - sessionId: {}, 현재 상태: {}",
                                    sessionId, stateMachineService.getCurrentState(sessionId));
                        }
                        canService.recoverUserCan(userId, 1);
                        return;
                    }

                    // 완전한 RAG 응답을 Redis에 저장
                    String fullRagResponse = ragResponseBuilder.toString();
                    RedisMessageDto ragRedisMessageDto = redisMessageMapper.toDtoLlm(fullRagResponse);
                    chatRedisService.saveMessage(sessionId, ragRedisMessageDto);

                    // SSE 완료 이벤트
                    emitComplete(sessionId);

                    // 별도 트랜잭션 -> 콜백은 요청 스레드 밖에서 실행되어 @Transactional 이 안 먹으므로 수동으로 경계 지정
                    transactionTemplate.executeWithoutResult(status -> {
                        QuestionResult questionResult = questionResultRepository.findById(questionResultId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_RESULT_NOT_FOUND));

                        questionResult.markOpener();

                        ExamResult examResult = questionResult.getExamResult();
                        examResult.recordOpenerUsage();

                        examResultRepository.save(examResult);
                        questionResultRepository.save(questionResult);
                    });

                    // State Machine: STREAM_COMPLETE (STREAMING → COMPLETED)
                    if (!stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_COMPLETE)) {
                        log.warn("[OpenerAnalysis] STREAM_COMPLETE 전이 거부 - sessionId: {}, 현재 상태: {}",
                                sessionId, stateMachineService.getCurrentState(sessionId));
                    }
                })
                .doOnError(error -> {
                    if (error instanceof BusinessException be) {
                        ErrorCode errorCode = be.getErrorCode();
                        log.error("[OpenerAnalysis] 비즈니스 예외 - userId: {}, errorCode: {}",
                                userId, errorCode.getCode(), error);
                        emitError(sessionId, errorCode.getMessage(), errorCode.getCode());
                    } else {
                        log.error("[OpenerAnalysis] 예외 발생 - userId: {}", userId, error);
                        emitError(sessionId,
                                ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                                ErrorCode.INTERNAL_SERVER_ERROR.getCode());
                    }
                    if (!stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_ERROR)) {
                        log.warn("[OpenerAnalysis] STREAM_ERROR 전이 거부 - sessionId: {}, 현재 상태: {}",
                                sessionId, stateMachineService.getCurrentState(sessionId));
                    }
                    // 실패 시 Can 복구 -> 응답을 못 받았는데 비용만 차감되는 불공정 방지
                    canService.recoverUserCan(userId, 1);
                })
                .subscribe();
    }

    // ── SSE 이벤트 emit 헬퍼 ─────────────────────────────

    private void emitConnected(Long sessionId) {
        emitEvent(sessionId, "connected",
                SseMessageResponse.builder()
                        .type("connected")
                        .sessionId(sessionId.toString())
                        .build());
    }

    private void emitChunk(Long sessionId, String chunk) {
        emitEvent(sessionId, "message",
                SseMessageResponse.builder()
                        .type("chunk")
                        .sessionId(sessionId.toString())
                        .chunk(chunk)
                        .build());
    }

    private void emitComplete(Long sessionId) {
        emitEvent(sessionId, "complete",
                SseMessageResponse.builder()
                        .type("complete")
                        .sessionId(sessionId.toString())
                        .build());
    }

    private void emitError(Long sessionId, String error, String errorCode) {
        emitEvent(sessionId, "error",
                SseMessageResponse.builder()
                        .type("error")
                        .sessionId(sessionId.toString())
                        .error(error)
                        .errorCode(errorCode)
                        .build());
    }

    // Sink에 ServerSentEvent 발행
    private void emitEvent(Long sessionId, String eventName, SseMessageResponse message) {
        Sinks.Many<ServerSentEvent<String>> sink = sinks.get(sessionId);
        if (sink == null) {
            log.warn("[SSE] Sink 없음 - sessionId: {}, event: {}", sessionId, eventName);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            ServerSentEvent<String> sse = ServerSentEvent.<String>builder()
                    .event(eventName)
                    .data(json)
                    .build();

            Sinks.EmitResult result = sink.tryEmitNext(sse);
            if (result.isFailure()) {
                log.warn("[SSE] emit 실패 - sessionId: {}, event: {}, result: {}",
                        sessionId, eventName, result);
            }
        } catch (JsonProcessingException e) {
            log.error("[SSE] JSON 직렬화 실패 - sessionId: {}, event: {}", sessionId, eventName, e);
        }
    }

    // ── 유틸리티 ─────────────────────────────────────────

    // Question 정보를 문자열로 변환
    private String buildProblemContext(Question question) {
        if (question.getPassages() == null || question.getPassages().isEmpty()) {
            throw new BusinessException(ErrorCode.QUESTION_HAS_NO_PASSAGES);
        }

        StringBuilder context = new StringBuilder();
        context.append("문제 번호: ").append(question.getQuestionNo()).append("\n");
        context.append("카테고리: ").append(question.getCategory()).append("\n");
        context.append("배점: ").append(question.getPoint()).append("점\n\n");

        context.append("=== 문제 지문 ===\n");
        for (Passage passage : question.getPassages()) {
            context.append(passage.content()).append("\n");
        }
        context.append("\n");

        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            context.append("=== 선택지 ===\n");
            for (var option : question.getOptions()) {
                context.append(option.order()).append(". ").append(option.content()).append("\n");
            }
            context.append("\n");
        }

        if (question.getAnswer() != null) {
            context.append("=== 정답 ===\n");
            context.append("정답: ").append(question.getAnswer()).append("번\n");
        }

        return context.toString();
    }

    // readOnly=true -> 쓰기 없는 조회임을 명시, 더티체킹/플러시 생략으로 성능 이점
    @Override
    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistoryByQuestionResultId(Long questionResultId) {
        ChatMessage chatMessage = chatMessageRepository.findByQuestionResultId(questionResultId)
                .orElse(null);

        if (chatMessage == null) {
            return ChatHistoryResponse.builder()
                    .chat(List.of())
                    .summary(null)
                    .build();
        }

        List<ChatMessageDto> chatDtos = IntStream.range(0, chatMessage.getMessages().size())
                .mapToObj(index -> {
                    ChatMessageContent content = chatMessage.getMessages().get(index);
                    return ChatMessageDto.builder()
                            .order(index + 1)
                            .role(content.getRole())
                            .content(content.getContent())
                            .timestamp(content.getTimestamp())
                            .build();
                })
                .toList();

        return ChatHistoryResponse.builder()
                .chat(chatDtos)
                .summary(chatMessage.getSummary())
                .build();
    }

    // @Scheduled 주기 ping -> 유휴 연결이 프록시/방화벽에 끊기는 것 방지 + 죽은 연결 조기 감지/정리
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        if (sinks.isEmpty()) {
            return;
        }

        List<Long> deadSessions = new ArrayList<>();

        sinks.forEach((sessionId, sink) -> {
            ServerSentEvent<String> heartbeat = ServerSentEvent.<String>builder()
                    .comment("ping")
                    .build();

            Sinks.EmitResult result = sink.tryEmitNext(heartbeat);
            if (result.isFailure()) {
                deadSessions.add(sessionId);
                log.debug("[SSE] Heartbeat 전송 실패, 연결 제거 - sessionId: {}", sessionId);
            }
        });

        deadSessions.forEach(sessionId -> {
            sinks.remove(sessionId);
            stateMachineService.removeMachine(sessionId);
        });

        if (!deadSessions.isEmpty()) {
            log.info("[SSE] Heartbeat로 {}개의 죽은 연결 정리, 남은 연결 수: {}",
                    deadSessions.size(), sinks.size());
        }
    }
}
