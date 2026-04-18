package spring.backend.domain.chat.service.impl;

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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
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

    // SSE 타임아웃 (1시간)
    private static final Long SSE_TIMEOUT = 60 * 60 * 1000L;
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

    // SSE 연결 관리 (sessionId → SseEmitter)
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

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
            ChatSessionStateMachineService stateMachineService
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
    }

    @Override
    @Transactional
    public SseEmitter connectSession(Long sessionId, UUID userId) {

        log.info("[SSE] 연결 요청 시작 - sessionId: {}, userId: {}", sessionId, userId);

        // 스프링 인메모리 힙에 sessionId로 운영중인 SSE 연결 조회
        SseEmitter sseEmitter = emitters.get(sessionId);

        // 기존 SSE 연결이 있으면 재사용 (재연결)
        if (sseEmitter != null) {
            log.info("[SSE] 기존 연결 재사용 - sessionId: {}, userId: {}, 현재 활성 연결 수: {}",
                    sessionId, userId, emitters.size());

            // DB에서 sessionId와 userId로 세션 권한 검증
            chatRedisService.validateSessionOwner(sessionId, userId);

            log.info("[SSE] 세션 권한 검증 완료 - sessionId: {}", sessionId);

            return sseEmitter;
        }

        // 새 SSE 연결 생성
        try {
            log.info("[SSE] 새로운 연결 생성 시작 - sessionId: {}, userId: {}, timeout: {}ms",
                    sessionId, userId, SSE_TIMEOUT);

            SseEmitter newEmitter = new SseEmitter(SSE_TIMEOUT);

            // 세션 해제 동작
            newEmitter.onCompletion(() -> {
                emitters.remove(sessionId);
                stateMachineService.removeMachine(sessionId);
                log.info("[SSE] 연결 정상 종료 (onCompletion) - sessionId: {}, 남은 연결 수: {}",
                        sessionId, emitters.size());
            });

            // 타임아웃시 ConcurrentHashMap 에서 emitter 제거
            newEmitter.onTimeout(() -> {
                emitters.remove(sessionId);
                stateMachineService.removeMachine(sessionId);
                log.warn("[SSE] 연결 타임아웃 ({}ms 초과) - sessionId: {}, 남은 연결 수: {}",
                        SSE_TIMEOUT, sessionId, emitters.size());
            });

            // 세션 예외 발생 처리 (클라이언트 연결 끊김은 정상적인 상황이므로 DEBUG로 처리)
            newEmitter.onError(e -> {
                emitters.remove(sessionId);
                stateMachineService.removeMachine(sessionId);
                // 클라이언트 연결 끊김 관련 예외는 DEBUG 레벨로 처리
                if (isClientDisconnectException(e)) {
                    log.debug("[SSE] 클라이언트 연결 끊김 - sessionId: {}, 오류: {}, 남은 연결 수: {}",
                            sessionId, e.getMessage(), emitters.size());
                } else {
                    log.warn("[SSE] 연결 오류 발생 - sessionId: {}, 오류: {}, 남은 연결 수: {}",
                            sessionId, e.getMessage(), emitters.size());
                }
            });

            User user = userRepository.findUserById(userId);

            // User가 존재하지 않으면 예외 발생
            if (user == null) {
                log.error("[SSE] ❌ 사용자를 찾을 수 없음 - userId: {}", userId);
                throw new BusinessException(ErrorCode.USER_NOT_FOUND);
            }

            log.debug("[SSE] 사용자 조회 완료 - userId: {}, userName: {}", userId, user.getName());

            // 세션용 레디스 초기화
            chatRedisService.initializeSession(sessionId, userId);

            // State Machine: 연결 성공 상태 전이 (IDLE → CONNECTED)
            stateMachineService.getOrCreateMachine(sessionId);
            stateMachineService.sendEvent(sessionId, ChatSessionEvent.CONNECT_SUCCESS);

            // 모든 인프라 준비 완료 후 Emitter를 Map에 등록
            emitters.put(sessionId, newEmitter);

            log.debug("[SSE] Emitter 저장 완료 - sessionId: {}", sessionId);

            // 클라이언트에게 커넥션 연결 성공 알림
            sendSseConnected(newEmitter, sessionId.toString());

            log.info("[SSE] ✅ 새 연결 성공 - sessionId: {}, userId: {}, userName: {}, 현재 활성 연결 수: {}",
                    sessionId, userId, user.getName(), emitters.size());

            return newEmitter;

        } catch (Exception e) {
            // 오류시 새롭게 생성된 세션 삭제
            emitters.remove(sessionId);

            // State Machine: 연결 실패
            stateMachineService.removeMachine(sessionId);
            log.error("[SSE] ❌ 연결 생성 실패 - sessionId: {}, userId: {}, 오류: {}",
                    sessionId, userId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SESSION_INITIALIZE_FAIL);
        }
    }

    @Override
    @Transactional
    public void disconnectSession(Long sessionId, UUID userId) {

        log.info("[SSE] 세션 해제 요청 - sessionId: {}, userId: {}", sessionId, userId);

        // 스프링 인메모리 힙에 기존 SSE 연결이 있으는지 확인
        if (emitters.containsKey(sessionId)) {

            // DB에서 sessionId와 userId로 세션 권한 검증
            chatRedisService.validateSessionOwner(sessionId, userId);

            log.debug("[SSE] 세션 권한 검증 완료 - sessionId: {}", sessionId);

            // 검증 통과시 emitter 삭제
            emitters.remove(sessionId);

            // redis에서 세션 삭제
            chatRedisService.deleteSession(sessionId);

            // State Machine: 세션 종료 (→ IDLE) + 제거
            stateMachineService.sendEvent(sessionId, ChatSessionEvent.CLOSE);
            stateMachineService.removeMachine(sessionId);

            log.info("[SSE] ✅ 세션 해제 완료 - sessionId: {}, userId: {}, 남은 연결 수: {}",
                    sessionId, userId, emitters.size());
        } else {
            log.warn("[SSE] ❌ 세션 해제 실패 - 존재하지 않는 세션 - sessionId: {}, userId: {}, 활성 세션 목록: {}",
                    sessionId, userId, emitters.keySet());
            // 본인 세션이 아닌 오류
            throw new BusinessException(ErrorCode.INVALID_SESSION);
        }
    }

    @Async
    @Override
    public void processMessageAsync(ChatSendRequest req, UUID userId) {

        Long sessionId = req.sessionId();
        String userMessage = req.message();

        log.info("[Chat] 메시지 처리 시작 - sessionId: {}, userId: {}, 현재 활성 연결 수: {}",
                sessionId, userId, emitters.size());

        // 스프링 인메모리 힙에 sessionId로 운영중인 SSE 연결 조회
        SseEmitter sseEmitter = emitters.get(sessionId);

        // SSE 연결 유지 확인
        if (sseEmitter == null) {
            log.error("[Chat] ❌ SSE 연결 없음 - sessionId: {}, 활성 세션 목록: {}",
                    sessionId, emitters.keySet());
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

        try {
            // 사용자 메시지를 Redis에 저장
            RedisMessageDto userRedisMessageDto = redisMessageMapper.toDtoUser(req);
            chatRedisService.saveMessage(sessionId, userRedisMessageDto);

            // LLM 응답을 수집할 StringBuilder
            StringBuilder llmResponseBuilder = new StringBuilder();
            AtomicBoolean streamStarted = new AtomicBoolean(false);

            // LLM 스트리밍 호출 (Circuit Breaker로 보호됨 - Phase 2)
            llmService.chatStream(
                    sessionId.toString(),
                    userMessage,
                    chunk -> {
                        // 첫 청크면 State Machine: STREAM_START
                        if (streamStarted.compareAndSet(false, true)) {
                            stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_START);
                        }

                        // 각 청크를 SSE로 전송
                        sendSseChunk(sseEmitter, sessionId.toString(), chunk);

                        // 전체 응답 수집
                        llmResponseBuilder.append(chunk);
                    }
            );

            // 완전한 LLM 응답을 Redis에 저장
            String fullLlmResponse = llmResponseBuilder.toString();
            RedisMessageDto llmRedisMessageDto = redisMessageMapper.toDtoLlm(fullLlmResponse);
            chatRedisService.saveMessage(sessionId, llmRedisMessageDto);

            // 완료 이벤트 전송
            sendSseComplete(sseEmitter, sessionId.toString());

            // State Machine: STREAM_COMPLETE (STREAMING → COMPLETED)
            stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_COMPLETE);

        } catch (Exception e) {
            log.error("[Chat] 메시지 처리 중 예외 발생 - sessionId: {}", sessionId, e);
            sendSseError(sseEmitter, sessionId.toString(), e.getMessage());
            stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_ERROR);
        }
    }

    // 대화내용 redis 적재
    @Override
    public void saveMessagesAsync(ChatSaveRequest req, UUID userId) {

        Long sessionId = req.sessionId();
        Long questionResultId = req.questionResultId();

        // 스프링 인메모리 힙에 sessionId로 운영중인 SSE 연결 조회
        SseEmitter sseEmitter = emitters.get(sessionId);

        // SSE 연결 유지 확인
        if (sseEmitter == null) {
            throw new BusinessException(ErrorCode.SESSION_EXPIRED);
        }

        // 권한 검증
        chatRedisService.validateSessionOwner(sessionId, userId);

        // RabbitMQ를 통해 메시지 저장 이벤트 발행
        // 실제 저장은 ChatMessageConsumer에서 비동기로 처리
        // Redis 삭제도 Consumer에서 저장 완료 후 수행
        chatMessageProducer.publishSaveMessageEvent(sessionId, userId, questionResultId);

        log.info("[Chat] 대화 저장 이벤트 발행 완료 - sessionId: {}", sessionId);
    }

    // sse 연결 성공 알림
    private void sendSseConnected(SseEmitter emitter, String sessionId) {
        try {
            SseMessageResponse message =
                    SseMessageResponse.builder()
                            .type("connected")
                            .sessionId(sessionId)
                            .build();

            emitter.send(
                    SseEmitter.event()
                            .name("connected")
                            .data(objectMapper.writeValueAsString(message)));

            log.debug("[SSE] 연결 성공 이벤트 전송 완료 - sessionId: {}", sessionId);

        } catch (Exception e) {
            log.warn("[SSE] 연결 성공 이벤트 전송 실패 - sessionId: {}, error: {}", sessionId, e.getMessage());
        }
    }

    // sse 청크 전송
    private void sendSseChunk(SseEmitter emitter, String sessionId, String chunk) {
        try {
            SseMessageResponse message =
                    SseMessageResponse.builder()
                            .type("chunk")
                            .sessionId(sessionId)
                            .chunk(chunk)
                            .build();

            emitter.send(
                    SseEmitter.event()
                            .name("message")
                            .data(objectMapper.writeValueAsString(message)));

        } catch (Exception e) {
        }
    }

    // sse 완료 알림
    private void sendSseComplete(SseEmitter emitter, String sessionId) {
        try {
            SseMessageResponse message =
                    SseMessageResponse.builder()
                            .type("complete")
                            .sessionId(sessionId)
                            .build();

            emitter.send(
                    SseEmitter.event()
                            .name("complete")
                            .data(objectMapper.writeValueAsString(message)));

        } catch (Exception e) {
        }
    }

    // sse 에러 알림 (에러 코드 없이)
    private void sendSseError(SseEmitter emitter, String sessionId, String error) {
        sendSseError(emitter, sessionId, error, null);
    }

    // sse 에러 알림 (에러 코드 포함)
    private void sendSseError(SseEmitter emitter, String sessionId, String error, String errorCode) {
        try {
            SseMessageResponse message =
                    SseMessageResponse.builder()
                            .type("error")
                            .sessionId(sessionId)
                            .error(error)
                            .errorCode(errorCode)
                            .build();

            emitter.send(
                    SseEmitter.event()
                            .name("error")
                            .data(objectMapper.writeValueAsString(message)));

        } catch (Exception e) {
        }
    }

    @Async
    @Transactional
    @Override
    public void openerAnalysis(OpenerAnalysisRequest req, UUID userId) {

        Long sessionId = req.sessionId();
        Long questionId = req.questionId();
        Long questionResultId = req.questionResultId();

        // SSE 연결 조회
        SseEmitter sseEmitter = emitters.get(sessionId);
        if (sseEmitter == null) {
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

        // Can 차감 시도 - 실패 시 SSE 에러 전송 후 조기 반환
        try {
            canService.useUserCan(userId, 1);
        } catch (BusinessException e) {
            ErrorCode errorCode = e.getErrorCode();
            log.warn("[OpenerAnalysis] Can 차감 실패 - userId: {}, errorCode: {}", userId, errorCode.getCode());
            sendSseError(sseEmitter, sessionId.toString(), errorCode.getMessage(), errorCode.getCode());
            return;
        }

        try {
            // 1. 사용자의 오프너 분석 요청 메시지를 Redis에 저장
            RedisMessageDto userRequestMessage = RedisMessageDto.builder()
                    .chatRole(ChatRole.USER)
                    .message("오프너 분석을 요청했습니다.")
                    .timestamp(java.time.LocalDateTime.now())
                    .build();
            chatRedisService.saveMessage(sessionId, userRequestMessage);

            // Question 조회
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

            // Passages를 문자열로 변환
            String problemContext = buildProblemContext(question);

            // RAG 응답을 수집할 StringBuilder
            StringBuilder ragResponseBuilder = new StringBuilder();
            AtomicBoolean ragStreamStarted = new AtomicBoolean(false);

            // RAG 서비스를 통해 유사 문제 생성 (스트리밍, Circuit Breaker로 보호됨 - Phase 2)
            ragService.generateSimilarProblemStream(
                    problemContext,
                    chunk -> {
                        // 첫 청크면 State Machine: STREAM_START
                        if (ragStreamStarted.compareAndSet(false, true)) {
                            stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_START);
                        }

                        // 각 청크를 SSE로 전송
                        sendSseChunk(sseEmitter, sessionId.toString(), chunk);

                        // 전체 응답 수집
                        ragResponseBuilder.append(chunk);
                    }
            );

            // 2. 완전한 RAG 응답을 Redis에 저장
            String fullRagResponse = ragResponseBuilder.toString();
            RedisMessageDto ragRedisMessageDto = redisMessageMapper.toDtoLlm(fullRagResponse);
            chatRedisService.saveMessage(sessionId, ragRedisMessageDto);

            // 완료 이벤트 전송
            sendSseComplete(sseEmitter, sessionId.toString());

            // QuestionResult 업데이트 (isOpener = true)
            // @Transactional 덕분에 LAZY 로딩 가능
            QuestionResult questionResult = questionResultRepository.findById(questionResultId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_RESULT_NOT_FOUND));

            // QuestionResult 상태 업데이트
            questionResult.markOpener();

            // ExamResult 상태 업데이트
            // @Transactional 덕분에 LAZY 프록시가 자동 초기화되고 dirty checking 작동
            ExamResult examResult = questionResult.getExamResult();
            examResult.recordOpenerUsage();

            // 트랜잭션 커밋 시 자동 저장되지만, 명시적 호출로 의도 명확화
            examResultRepository.save(examResult);
            questionResultRepository.save(questionResult);

            // State Machine: STREAM_COMPLETE (STREAMING → COMPLETED)
            stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_COMPLETE);

        } catch (BusinessException e) {
            // 비즈니스 예외: 에러 코드 포함하여 전송
            ErrorCode errorCode = e.getErrorCode();
            log.error("[OpenerAnalysis] 비즈니스 예외 발생 - userId: {}, errorCode: {}", userId, errorCode.getCode(), e);
            sendSseError(sseEmitter, sessionId.toString(), errorCode.getMessage(), errorCode.getCode());
            stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_ERROR);
            canService.recoverUserCan(userId, 1);
        } catch (Exception e) {
            // 기타 예외: 내부 서버 오류로 처리
            log.error("[OpenerAnalysis] 예외 발생 - userId: {}", userId, e);
            sendSseError(sseEmitter, sessionId.toString(),
                    ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                    ErrorCode.INTERNAL_SERVER_ERROR.getCode());
            stateMachineService.sendEvent(sessionId, ChatSessionEvent.STREAM_ERROR);
            canService.recoverUserCan(userId, 1);
        }
    }

    /**
     * 클라이언트 연결 끊김 관련 예외인지 확인 이러한 예외들은 정상적인 상황이므로 ERROR 대신 DEBUG로 처리
     */
    private boolean isClientDisconnectException(Throwable e) {
        if (e == null) {
            return false;
        }
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("broken pipe") ||
                    lowerMessage.contains("connection reset") ||
                    lowerMessage.contains("client disconnected") ||
                    lowerMessage.contains("disconnected client") ||
                    lowerMessage.contains("closed") ||
                    lowerMessage.contains("aborted");
        }
        // 원인(cause)도 확인
        return isClientDisconnectException(e.getCause());
    }

    // Question의 모든 정보를 하나의 문자열로 변환
    private String buildProblemContext(Question question) {
        if (question.getPassages() == null || question.getPassages().isEmpty()) {
            throw new BusinessException(ErrorCode.QUESTION_HAS_NO_PASSAGES);
        }

        StringBuilder context = new StringBuilder();
        context.append("문제 번호: ").append(question.getQuestionNo()).append("\n");
        context.append("카테고리: ").append(question.getCategory()).append("\n");
        context.append("배점: ").append(question.getPoint()).append("점\n\n");

        // Passages (지문)
        context.append("=== 문제 지문 ===\n");
        for (Passage passage : question.getPassages()) {
            context.append(passage.content()).append("\n");
        }
        context.append("\n");

        // Options (선택지)
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            context.append("=== 선택지 ===\n");
            for (var option : question.getOptions()) {
                context.append(option.order()).append(". ").append(option.content()).append("\n");
            }
            context.append("\n");
        }

        // Answer (정답)
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

        // 채팅 기록이 없는 경우 빈 응답 반환
        if (chatMessage == null) {
            return ChatHistoryResponse.builder()
                    .chat(List.of())
                    .summary(null)
                    .build();
        }

        // ChatMessageContent → ChatMessageDto 변환 (order 추가)
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

    /**
     * SSE Heartbeat - 30초마다 모든 활성 연결에 ping 전송 유휴 연결이 중간 장비(Nginx, 방화벽 등)에 의해 끊어지는 것을 방지
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        List<Long> deadSessions = new ArrayList<>();

        emitters.forEach((sessionId, emitter) -> {
            try {
                // SSE comment로 heartbeat 전송 (클라이언트에서 이벤트로 처리되지 않음)
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                // 전송 실패 시 죽은 연결로 표시
                deadSessions.add(sessionId);
                log.debug("[SSE] Heartbeat 전송 실패, 연결 제거 - sessionId: {}", sessionId);
            }
        });

        // 죽은 연결 정리
        deadSessions.forEach(emitters::remove);

        if (!deadSessions.isEmpty()) {
            log.info("[SSE] Heartbeat로 {}개의 죽은 연결 정리, 남은 연결 수: {}",
                    deadSessions.size(), emitters.size());
        }
    }
}
