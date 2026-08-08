package spring.backend.domain.chat.messaging;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import spring.backend.domain.chat.dto.message_dto.ChatMessageSaveEvent;
import spring.backend.shared.infrastructure.messaging.config.RabbitMQConfig;


// 저장 작업을 직접 하지 않고 이벤트만 발행 -> 응답 경로에서 DB I/O 분리, 컨슈머가 비동기로 처리
@Component
@RequiredArgsConstructor
public class ChatMessageProducer {

    // RabbitTemplate 주입 -> 발행 보일러플레이트 감추고 컨버터 설정 재사용
    private final RabbitTemplate rabbitTemplate;

    // 페이로드에 id 만 담음 -> 큐에 대용량 대화 본문 싣지 않고, 실제 데이터는 컨슈머가 Redis 에서 조회
    public void publishSaveMessageEvent(Long sessionId, UUID userId, Long questionResultId) {
        ChatMessageSaveEvent event = ChatMessageSaveEvent.builder()
                .sessionId(sessionId)
                .userId(userId)
                .questionResultId(questionResultId)
                .publishedAt(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_MESSAGE_EXCHANGE,
                RabbitMQConfig.CHAT_MESSAGE_SAVE_ROUTING_KEY,
                event
        );
    }
}
