package spring.backend.domain.chat.messaging;

import java.util.List;
import lombok.RequiredArgsConstructor;
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
import spring.backend.shared.infrastructure.messaging.config.RabbitMQConfig;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@Component
@RequiredArgsConstructor
public class ChatMessageConsumer {

    private final ChatRedisService chatRedisService;
    private final RedisMessageMapper redisMessageMapper;
    private final ChatMessageRepository chatMessageRepository;

    @RabbitListener(queues = RabbitMQConfig.CHAT_MESSAGE_SAVE_QUEUE)
    @Transactional
    public void handleSaveMessageEvent(ChatMessageSaveEvent event) {
        Long sessionId = event.sessionId();

        try {

            // 권한 검증
            chatRedisService.validateSessionOwner(sessionId, event.userId());

            // Redis에서 세션 메시지 조회
            List<RedisMessageDto> redisMessageDtos = chatRedisService.getSessionMessages(sessionId);

            // 메시지가 없으면 저장하지 않음
            if (redisMessageDtos == null || redisMessageDtos.isEmpty()) {
                throw new BusinessException(ErrorCode.NO_MESSAGE_STORED);
            }

            // MessageDto 리스트를 ChatMessageContent 리스트로 변환
            List<ChatMessageContent> messageContents = redisMessageMapper.toEntityList(redisMessageDtos);

            // ChatMessage 엔티티 생성 및 저장
            ChatMessage chatMessage = ChatMessage.createFromSession(messageContents);
            chatMessageRepository.save(chatMessage);


        } catch (BusinessException e) {
            throw e;

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        }
    }
}
