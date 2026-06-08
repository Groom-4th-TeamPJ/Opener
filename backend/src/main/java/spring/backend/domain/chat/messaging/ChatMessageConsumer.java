package spring.backend.domain.chat.messaging;

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

            // 메시지 없음 = TTL 만료된 정상 상황 -> 예외 던지면 DLQ 로 가므로, 정상 ACK 처리해 큐에서 제거
            if (redisMessages == null || redisMessages.isEmpty()) {
                log.warn("[RabbitMQ] Redis 세션 만료 또는 메시지 없음 - sessionId: {}. 메시지 무시", sessionId);
                return;
            }

            // 권한 검증 (Redis에 데이터가 있는 경우에만 검증)
            try {
                chatRedisService.validateSessionOwner(sessionId, event.userId());
            } catch (BusinessException e) {
                if (e.getErrorCode() == ErrorCode.INVALID_SESSION) {
                    log.warn("[RabbitMQ] 세션 권한 검증 실패 - sessionId: {}, userId: {}. 메시지 무시",
                            sessionId, event.userId());
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
