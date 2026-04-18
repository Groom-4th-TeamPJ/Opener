package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

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

    // SSE 연결 관리 (sessionId → Sink) — Sink에 emit하면 구독 중인 Flux로 SSE 전송
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
            @Qualifier("chatRedisTemplate") StringRedisTemplate redisTemplate,
            JpaQuestionRepository questionRepository,
            QuestionResultRepository questionResultRepository,
            ExamResultRepository examResultRepository,
            CanService canService,
            ChatSessionStateMachineService stateMachineService,
            PlatformTransactionManager transactionManager
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
    }

    @Override
    @Transactional
    public Flux<ServerSentEvent<String>> connectSession(Long sessionId, UUID userId) {

        log.info("[SSE] 연결 요청 시작 - sessionId: {}, userId: {}", sessionId, userId);

        // 기존 Sink가 있으면 재사용 (재연결)
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
            stateMachineService.sendEvent(sessionId, ChatSessionEvent.CONNECT_SUCCESS);

            // Sink 생성 — multicast + backpressure buffer
            Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();

            // 모든 인프라 준비 완료 후 Sink를 Map에 등록
            sinks.put(sessionId, sink);

            log.debug("[SSE] Sink 저장 완료 - sessionId: {}", sessionId);

            // 클라이언트에게 연결 성공 알림
            emitConnected(sessionId);

            log.info("[SSE] 새 연결 성공 - sessionId: {}, userId: {}, userName: {}, 현재 활성 연결 수: {}",
                    sessionId, userId, user.getName(), sinks.size());

            // Flux 반환 — 클라이언트 연결 해제 시 자동 정리
            return sink.asFlux()
                    .doOnCancel(() -> {
                        sinks.remove(sessionId);
                        stateMachineService.removeMachine(sessionId);
                        log.info("[SSE] 클라이언트 연결 해제 (cancel) - sessionId: {}, 남은 연결 수: {}",
                                sessionId, sinks.size());
                    });

        } catch (Exception e) {
            sinks.remove(sessionId);
            stateMachineService.removeMachine(sessionId);
            log.error("[SSE] 연결 생성 실패 - sessionId: {}, userId: {}, 오류: {}",
                    sessionId, userId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SESSION_INITIALIZE_FAIL);
        }
    }

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
            stateMachineService.sendEvent(sessionId, ChatSessionEvent.CLOSE);
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

        // LLM Flux를 non-blocking subscribe
        llmService.chatStream(sessionId.toString(), userMessage)
                .doOnNext(chunk -> {
                    // 첫 청크 → STREAM_START 상태 전이
                    if (streamStarted.compareAndSet(false, true)) {
                        stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_START);
                    }

                    // SSE 청크 전송
                    emitChunk(sessionId, chunk);

                    // 전체 응답 수집
                    llmResponseBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    // 완전한 LLM 응답을 Redis에 저장
                    String fullLlmResponse = llmResponseBuilder.toString();
                    RedisMessageDto llmRedisMessageDto = redisMessageMapper.toDtoLlm(fullLlmResponse);
                    chatRedisService.saveMessage(sessionId, llmRedisMessageDto);

                    // SSE 완료 이벤트
                    emitComplete(sessionId);

                    // State Machine: STREAM_COMPLETE (STREAMING → COMPLETED)
                    stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_COMPLETE);
                })
                .doOnError(error -> {
                    log.error("[Chat] 메시지 처리 중 예외 발생 - sessionId: {}", sessionId, error);
                    emitError(sessionId, error.getMessage(), null);
                    stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_ERROR);
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

        // RabbitMQ를 통해 메시지 저장 이벤트 발행
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

        // Can 차감 시도 — 실패 시 SSE 에러 전송 후 조기 반환
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
                .doOnNext(chunk -> {
                    // 첫 청크 → STREAM_START 상태 전이
                    if (ragStreamStarted.compareAndSet(false, true)) {
                        stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_START);
                    }

                    // SSE 청크 전송
                    emitChunk(sessionId, chunk);

                    // 전체 응답 수집
                    ragResponseBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    // 완전한 RAG 응답을 Redis에 저장
                    String fullRagResponse = ragResponseBuilder.toString();
                    RedisMessageDto ragRedisMessageDto = redisMessageMapper.toDtoLlm(fullRagResponse);
                    chatRedisService.saveMessage(sessionId, ragRedisMessageDto);

                    // SSE 완료 이벤트
                    emitComplete(sessionId);

                    // QuestionResult, ExamResult 업데이트 (별도 트랜잭션)
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
                    stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_COMPLETE);
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
                    stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_ERROR);
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

    // SSE Heartbeat — 30초마다 모든 활성 연결에 ping 전송
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
