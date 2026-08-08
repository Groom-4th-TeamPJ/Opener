package spring.backend.domain.chat.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.chat.dto.message_dto.ChatMessageSaveEvent;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
import spring.backend.domain.chat.mapper.RedisMessageMapper;
import spring.backend.domain.chat.model.entity.ChatMessage;
import spring.backend.domain.chat.model.entity.ChatMessageContent;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.shared.infrastructure.messaging.config.RabbitMQConfig;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageConsumer {

    // 모든 협력 객체를 final 로 주입 -> 불변 의존성 + 외부 재할당 차단으로 안전하게 사용
    private final ChatRedisService chatRedisService;
    private final RedisMessageMapper redisMessageMapper;
    private final ChatMessageRepository chatMessageRepository;
    private final QuestionResultRepository questionResultRepository;
    private final LlmService llmService;
    private final MeterRegistry meterRegistry;

    // 세션 TTL 과 같은 값 -> 이 시간을 넘긴 이벤트의 빈 버퍼는 정상 만료로 본다
    private static final Duration SESSION_TTL = Duration.ofHours(1);

    // @RabbitListener -> 큐 메시지를 자동 수신, 폴링 코드 없이 이벤트 도착 시 호출
    // @Transactional -> 요약/저장/Redis 삭제를 한 트랜잭션으로 묶어 부분 저장 방지
    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_SAVE_QUEUE)
    @Transactional
    public void handleSaveMessageEvent(ChatMessageSaveEvent event) {
        Long sessionId = event.sessionId();
        Long questionResultId = event.questionResultId();

        log.info("[RabbitMQ] 채팅 메시지 저장 이벤트 수신 - sessionId: {}, questionResultId: {}",
                sessionId, questionResultId);

        try {
            // Redis에서 세션 메시지 조회
            List<RedisMessageDto> redisMessages = chatRedisService.getSessionMessages(sessionId);

            // 버퍼가 비었을 때 정상 만료와 비정상 소실을 가른다
            // publishedAt 이 없으면(구 메시지) 판정 근거가 없으므로 안전한 쪽인 정상 만료로 본다
            if (redisMessages == null || redisMessages.isEmpty()) {
                Instant publishedAt = event.publishedAt();
                boolean expired = publishedAt == null
                        || Duration.between(publishedAt, Instant.now()).compareTo(SESSION_TTL) >= 0;

                if (expired) {
                    // TTL 을 넘겼으면 사라진 것이 정상 -> ACK 하되 사실은 남긴다
                    log.warn("[RabbitMQ] 세션 TTL 만료로 버퍼 비어 있음 - sessionId: {}", sessionId);
                    meterRegistry.counter("chat.persist.dropped", "reason", "expired").increment();
                    return;
                }

                // TTL 안인데 비어 있다 = 복제 유실·LRU 축출 -> 조용히 ACK 하면 유실을 유실로 알 수 없다
                log.error("[RabbitMQ] TTL 이내인데 버퍼가 비어 있음 - sessionId: {}, publishedAt: {}",
                        sessionId, publishedAt);
                meterRegistry.counter("chat.persist.dropped", "reason", "lost").increment();
                throw new BusinessException(ErrorCode.CHAT_BUFFER_LOST);
            }

            // 권한 검증 (Redis에 데이터가 있는 경우에만 검증)
            // 어드바이스(@RequireSessionOwner) 대상에서 제외 -> 이벤트는 userId 를 인자가 아니라 페이로드로 갖고
            // 검증 실패 시 처리도 HTTP 거부가 아니라 ACK/DLQ 결정이라 진입 규약이 다르다
            try {
                chatRedisService.validateSessionOwner(sessionId, event.userId());
            } catch (BusinessException e) {
                // INVALID_SESSION 이 SESSION_NOT_FOUND(404)·SESSION_ACCESS_DENIED(403) 로 갈렸으므로 둘 다 받는다
                // 코드만 비교하면 분기가 조용히 빠져 DLQ 로 새므로 분리 시 여기를 같이 고쳐야 한다
                if (e.getErrorCode() == ErrorCode.SESSION_NOT_FOUND
                        || e.getErrorCode() == ErrorCode.SESSION_ACCESS_DENIED
                        || e.getErrorCode() == ErrorCode.INVALID_SESSION) {
                    log.warn("[RabbitMQ] 세션 권한 검증 실패 - sessionId: {}, userId: {}. 메시지 무시",
                            sessionId, event.userId());
                    meterRegistry.counter("chat.persist.dropped", "reason", "unauthorized").increment();
                    // 권한 없는 경우도 정상 처리로 간주
                    return;
                }
                throw e;
            }

            // MessageDto 리스트를 ChatMessageContent 리스트로 변환 -> 현재 구성은 동일하지만 추후 확장성 고려
            List<ChatMessageContent> messageContents = redisMessageMapper.toEntityList(redisMessages);

            // questionResult 조회
            QuestionResult questionResult = questionResultRepository.findById(questionResultId)
                    .orElseThrow(() -> {
                        log.error("[RabbitMQ] QuestionResult 조회 실패 - questionResultId: {}", questionResultId);
                        return new BusinessException(ErrorCode.RESULT_NOT_FOUND);
                    });

            // 요약 진행
            String summary = llmService.summaryChat(sessionId.toString());

            // ChatMessage 엔티티 생성 및 저장
            ChatMessage chatMessage = ChatMessage.createFromSession(questionResult, messageContents, summary);
            chatMessageRepository.save(chatMessage);

            log.info("[RabbitMQ] 채팅 메시지 저장 완료 - sessionId: {}, 메시지 수: {}",
                    sessionId, messageContents.size());

            // DB 영속화 성공 후 Redis 정리 -> 버퍼는 임시 저장소이므로 영구 저장 완료 시점에 비움
            chatRedisService.deleteMessage(sessionId);

        } catch (BusinessException e) {
            log.error("[RabbitMQ] 비즈니스 예외 발생 - sessionId: {}, errorCode: {}, message: {}",
                    sessionId, e.getErrorCode(), e.getMessage());
            // 예외를 다시 던짐 -> 트랜잭션 롤백 + 메시지 DLQ 라우팅으로 유실 없이 추적
            throw e;

        } catch (Exception e) {
            log.error("[RabbitMQ] 예기치 않은 예외 발생 - sessionId: {}", sessionId, e);
            // 알 수 없는 예외도 BusinessException 으로 감싸 일관된 처리 경로 유지
            throw new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        }
    }
}
