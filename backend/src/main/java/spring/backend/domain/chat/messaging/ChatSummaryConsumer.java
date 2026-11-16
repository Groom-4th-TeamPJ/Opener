package spring.backend.domain.chat.messaging;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import spring.backend.domain.chat.adapter.RedisChatHistoryAdapter;
import spring.backend.domain.chat.dto.message_dto.ChatSummaryEvent;
import spring.backend.domain.chat.model.entity.ChatMessage;
import spring.backend.domain.chat.model.entity.ChatMessageContent;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.shared.infrastructure.messaging.config.RabbitMQConfig;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

// 저장 소비자가 요약 없이 저장한 행에 요약을 채운다 -> 4.5 초짜리 LLM 호출이 저장 지연에 더해지지 않는다
// 컨테이너 팩토리를 지정하지 않는다 -> 저장 리스너와 같은 팩토리의 동시 소비자 수와 재시도 제외 규칙을 그대로 쓴다
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSummaryConsumer {

    private final ChatMessageRepository chatMessageRepository;
    private final LlmService llmService;

    // @Transactional 없음 -> LLM 호출 동안 DB 커넥션을 쥐지 않는다, 읽기와 쓰기는 리포지토리 호출마다 짧게 끝난다
    @RabbitListener(queues = RabbitMQConfig.CHAT_SUMMARY_QUEUE)
    public void handleSummaryEvent(ChatSummaryEvent event) {
        // id 없는 이벤트는 다시 읽어도 같은 결과 -> 재시도 없이 DLQ 로 보낸다
        if (event == null || event.chatMessageId() == null) {
            log.error("[RabbitMQ] 요약 이벤트 id 누락 - event: {}", event);
            throw new AmqpRejectAndDontRequeueException("요약 이벤트 id 누락 - 재시도 불가");
        }

        Long chatMessageId = event.chatMessageId();
        // 저장은 커밋 뒤에 이벤트를 냈다 -> 행이 없으면 재시도해도 생기지 않으니 바로 DLQ 로 보낸다
        ChatMessage chatMessage = chatMessageRepository.findById(chatMessageId)
                .orElseThrow(() -> {
                    log.error("[RabbitMQ] 요약 대상 행 없음 - chatMessageId: {}", chatMessageId);
                    return new AmqpRejectAndDontRequeueException("요약 대상 행 없음 - 재시도 불가");
                });

        // 같은 이벤트가 다시 배달돼도 LLM 을 또 부르지 않는다
        if (chatMessage.getSummary() != null) {
            log.info("[RabbitMQ] 이미 요약된 행 - 건너뜀 - chatMessageId: {}", chatMessageId);
            return;
        }

        // 입력은 행에 저장된 메시지 -> Redis 버퍼는 저장 소비자가 이미 지웠고, 저장된 대화가 곧 요약 대상이다
        List<Message> messages = new ArrayList<>();
        for (ChatMessageContent content : chatMessage.getMessages()) {
            Message message = RedisChatHistoryAdapter.toMessage(content.getRole(), content.getContent());
            if (message != null) {
                messages.add(message);
            }
        }

        String summary = llmService.summarize(messages);
        if (summary == null) {
            // 일반 예외로 던진다 -> LLM 이 잠시 막힌 경우는 재시도가 회복시키고, 소진되면 DLQ 로 간다
            log.warn("[RabbitMQ] 요약 없음 - chatMessageId: {}", chatMessageId);
            throw new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);
        }

        chatMessage.updateSummary(summary);
        chatMessageRepository.save(chatMessage);
        log.info("[RabbitMQ] 요약 저장 완료 - chatMessageId: {}", chatMessageId);
    }
}
