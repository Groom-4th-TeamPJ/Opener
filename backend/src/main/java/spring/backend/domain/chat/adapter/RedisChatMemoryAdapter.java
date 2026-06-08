package spring.backend.domain.chat.adapter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
import spring.backend.domain.chat.service.spec.ChatRedisService;

/**
 * Spring AI의 ChatMemory 인터페이스를 구현하는 어댑터
 * 기존 ChatRedisService를 래핑하여 Spring AI의 멀티턴 대화 기능 제공
 */
// 어댑터 패턴 -> Spring AI 의 ChatMemory 규격에 우리 Redis 저장소를 끼워 맞춰
// LLM 호출 시 멀티턴 히스토리가 자동으로 주입되게 함 (Redis 코드 수정 없이 연동)
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemoryAdapter implements ChatMemory {

    // 기존 서비스 재사용 -> 저장 로직을 중복 구현하지 않고 위임만 함
    private final ChatRedisService chatRedisService;

    @Override
    public void add(String conversationId, List<Message> messages) {
        try {
            Long sessionId = Long.parseLong(conversationId);

            for (Message message : messages) {
                // Spring AI Message를 RedisMessageDto로 변환
                RedisMessageDto redisMessage = convertToRedisMessage(message);

                // 기존 ChatRedisService를 사용하여 Redis에 저장
                chatRedisService.saveMessage(sessionId, redisMessage);
            }

            log.debug("[ChatMemory] 메시지 저장 완료 - conversationId: {}, 메시지 수: {}",
                    conversationId, messages.size());

        // 저장 실패해도 예외 전파 안 함 -> 히스토리 누락이 LLM 응답 자체를 막지 않도록 함
        } catch (NumberFormatException e) {
            log.error("[ChatMemory] 잘못된 conversationId 형식 - conversationId: {}", conversationId, e);
        } catch (Exception e) {
            log.error("[ChatMemory] 메시지 저장 실패 - conversationId: {}", conversationId, e);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        try {
            Long sessionId = Long.parseLong(conversationId);

            // 기존 ChatRedisService를 사용하여 Redis에서 메시지 조회
            List<RedisMessageDto> redisMessages = chatRedisService.getSessionMessages(sessionId);

            // RedisMessageDto를 Spring AI Message로 변환
            List<Message> messages = new ArrayList<>();
            for (RedisMessageDto redisMsg : redisMessages) {
                Message message = convertToSpringAiMessage(redisMsg);
                if (message != null) {
                    messages.add(message);
                }
            }

            log.debug("[ChatMemory] 메시지 조회 완료 - conversationId: {}, 메시지 수: {}",
                    conversationId, messages.size());

            return messages;

        } catch (NumberFormatException e) {
            log.error("[ChatMemory] 잘못된 conversationId 형식 - conversationId: {}", conversationId, e);
            return List.of();
        } catch (Exception e) {
            log.error("[ChatMemory] 메시지 조회 실패 - conversationId: {}", conversationId, e);
            return List.of();
        }
    }

    @Override
    public void clear(String conversationId) {
        try {
            Long sessionId = Long.parseLong(conversationId);

            // 기존 ChatRedisService를 사용하여 메시지 삭제
            chatRedisService.deleteMessage(sessionId);

            log.debug("[ChatMemory] 메시지 삭제 완료 - conversationId: {}", conversationId);

        } catch (NumberFormatException e) {
            log.error("[ChatMemory] 잘못된 conversationId 형식 - conversationId: {}", conversationId, e);
        } catch (Exception e) {
            log.error("[ChatMemory] 메시지 삭제 실패 - conversationId: {}", conversationId, e);
        }
    }

    /**
     * Spring AI Message를 RedisMessageDto로 변환
     */
    private RedisMessageDto convertToRedisMessage(Message message) {
        ChatRole role;
        if (message instanceof UserMessage) {
            role = ChatRole.USER;
        } else if (message instanceof AssistantMessage) {
            role = ChatRole.LLM;
        } else {
            // 기본값으로 USER 사용
            role = ChatRole.USER;
        }

        return RedisMessageDto.builder()
                .chatRole(role)
                .message(message.getText())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * RedisMessageDto를 Spring AI Message로 변환
     */
    private Message convertToSpringAiMessage(RedisMessageDto redisMessage) {
        if (redisMessage.chatRole() == ChatRole.USER) {
            return new UserMessage(redisMessage.message());
        } else if (redisMessage.chatRole() == ChatRole.LLM) {
            return new AssistantMessage(redisMessage.message());
        } else {
            log.warn("[ChatMemory] 알 수 없는 ChatRole - role: {}", redisMessage.chatRole());
            return null;
        }
    }
}
