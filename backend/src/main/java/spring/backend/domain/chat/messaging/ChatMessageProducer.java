package spring.backend.domain.chat.messaging;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import spring.backend.domain.chat.dto.message_dto.ChatMessageSaveEvent;
import spring.backend.shared.infrastructure.messaging.config.RabbitMQConfig;


@Component
@RequiredArgsConstructor
public class ChatMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishSaveMessageEvent(Long sessionId, UUID userId, Long questionResultId) {
        try {
            ChatMessageSaveEvent event = ChatMessageSaveEvent.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .questionResultId(questionResultId)
                    .build();

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHAT_MESSAGE_EXCHANGE,
                    RabbitMQConfig.CHAT_MESSAGE_SAVE_ROUTING_KEY,
                    event
            );

        } catch (Exception e) {
            throw e;
        }
    }
}
