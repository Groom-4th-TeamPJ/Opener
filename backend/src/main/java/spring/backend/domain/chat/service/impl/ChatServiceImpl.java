package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
import spring.backend.domain.chat.dto.request.ChatSaveRequest;
import spring.backend.domain.chat.dto.request.ChatSendRequest;
import spring.backend.domain.chat.dto.request.OpenerAnalysisRequest;
import spring.backend.domain.chat.dto.response.ChatHistoryResponse;
import spring.backend.domain.chat.dto.response.SseMessageResponse;
import spring.backend.domain.chat.mapper.RedisMessageMapper;
import spring.backend.domain.chat.messaging.ChatMessageProducer;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.domain.chat.service.spec.ChatService;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.domain.chat.service.spec.RagService;
import spring.backend.domain.exam.model.dto.Passage;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.exam.repository.jpa.JpaQuestionRepository;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.domain.user.model.entity.User;
import spring.backend.domain.user.repository.spec.UserRepository;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@Service
public class ChatServiceImpl implements ChatService {

    // SSE 타임아웃 (5분)
    private static final Long SSE_TIMEOUT = 5 * 60 * 1000L;
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
            QuestionResultRepository questionResultRepository
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
    }

    @Override
    @Transactional
    public SseEmitter connectSession(Long sessionId, UUID userId) {

        // 스프링 인메모리 힙에 sessionId로 운영중인 SSE 연결 조회
        SseEmitter sseEmitter = emitters.get(sessionId);

        // 기존 SSE 연결이 있으면 재사용 (재연결)
        if (sseEmitter != null) {

            // DB에서 sessionId와 userId로 세션 권한 검증
            chatRedisService.validateSessionOwner(sessionId, userId);

            return sseEmitter;
        }

        // 새 SSE 연결 생성
        try {
            SseEmitter newEmitter = new SseEmitter(SSE_TIMEOUT);

            // 세션 해제 동작
            newEmitter.onCompletion(() -> emitters.remove(sessionId));

            // 타임아웃시 ConcurrentHashMap 에서 emitter 제거
            newEmitter.onTimeout(() -> emitters.remove(sessionId));

            // 세션 예외 발생 처리
            newEmitter.onError(e -> emitters.remove(sessionId));

            // sessionId로 Emitter 저장
            emitters.put(sessionId, newEmitter);

            User user = userRepository.findUserById(userId);

            // 세션용 레디스 초기화
            chatRedisService.initializeSession(sessionId, userId);

            return newEmitter;

        } catch (Exception e) {
            // 오류시 새롭게 생성된 세션 삭제
            emitters.remove(sessionId);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ChatHistoryResponse getHistory(Long sessionId, UUID userId) {
        // 권한 검증
        chatRedisService.validateSessionOwner(sessionId, userId);

        // Redis에서 메시지 조회
        List<RedisMessageDto> messages = chatRedisService.getSessionMessages(sessionId);

        return ChatHistoryResponse.of(sessionId, messages);
    }

    @Override
    @Transactional
    public void disconnectSession(Long sessionId, UUID userId) {

        // 스프링 인메모리 힙에 기존 SSE 연결이 있으는지 확인
        if (emitters.containsKey(sessionId)) {

            // DB에서 sessionId와 userId로 세션 권한 검증
            chatRedisService.validateSessionOwner(sessionId, userId);

            // 검증 통과시 emitter 삭제
            emitters.remove(sessionId);

            // redis에서 세션 삭제
            chatRedisService.deleteSession(sessionId);
        } else {
            // 본인 세션이 아닌 오류
            throw new BusinessException(ErrorCode.INVALID_SESSION);
        }
    }

    @Async
    @Override
    public void processMessageAsync(ChatSendRequest req, UUID userId) {

        Long sessionId = req.sessionId();
        String userMessage = req.message();

        // 스프링 인메모리 힙에 sessionId로 운영중인 SSE 연결 조회
        SseEmitter sseEmitter = emitters.get(sessionId);

        // SSE 연결 유지 확인
        if (sseEmitter == null) {
            throw new BusinessException(ErrorCode.SESSION_EXPIRED);
        }

        // 권한 검증
        chatRedisService.validateSessionOwner(sessionId, userId);

        try {
            // 사용자 메시지를 Redis에 저장
            RedisMessageDto userRedisMessageDto = redisMessageMapper.toDtoUser(req);
            chatRedisService.saveMessage(sessionId, userRedisMessageDto);

            // LLM 응답을 수집할 StringBuilder
            StringBuilder llmResponseBuilder = new StringBuilder();

            // LLM 스트리밍 호출
            llmService.chatStream(
                    sessionId.toString(),
                    userMessage,
                    chunk -> {
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

        } catch (Exception e) {
            sendSseError(sseEmitter, sessionId.toString(), e.getMessage());
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
        chatMessageProducer.publishSaveMessageEvent(sessionId, userId, questionResultId);
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

    // sse 에러 알림
    private void sendSseError(SseEmitter emitter, String sessionId, String error) {
        try {
            SseMessageResponse message =
                    SseMessageResponse.builder()
                            .type("error")
                            .sessionId(sessionId)
                            .error(error)
                            .build();

            emitter.send(
                    SseEmitter.event()
                            .name("error")
                            .data(objectMapper.writeValueAsString(message)));

        } catch (Exception e) {
        }
    }
}
