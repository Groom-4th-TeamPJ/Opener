package spring.backend.domain.chat.adapter;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
import spring.backend.domain.chat.service.spec.ChatHistoryProvider;
import spring.backend.domain.chat.service.spec.ChatRedisService;

/**
 * Redis 에 쌓인 대화 이력을 Spring AI 의 {@link Message} 로 변환해 제공하는 어댑터
 *
 * <p>읽기 전용이다. 쓰기는 {@code ChatServiceImpl} 이 {@code chatRedisService.saveMessage} 로 직접 수행한다.</p>
 */
// 어댑터 패턴 -> Redis 저장 포맷(RedisMessageDto)과 LLM 입력 포맷(Message) 사이의 변환을 한 곳에 가둠
// 이 클래스가 없으면 LLM 서비스마다 같은 변환을 반복하고 ChatRole 매핑이 갈라진다
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatHistoryAdapter implements ChatHistoryProvider {

    // 기존 서비스 재사용 -> 조회 로직을 중복 구현하지 않고 위임만 함
    private final ChatRedisService chatRedisService;

    // 윈도우 적용본 -> LLM 프롬프트에 실릴 컨텍스트는 최근 N 개면 충분
    @Override
    public List<Message> getHistoryBefore(String conversationId, long writtenLength) {
        try {
            Long sessionId = Long.parseLong(conversationId);

            // 지금 보내는 질문 직전까지만 읽는다 (partial 은 서비스 읽기가 이미 뺀다)
            List<RedisMessageDto> redisMessages = chatRedisService.getHistoryBefore(sessionId, writtenLength);

            List<Message> messages = new ArrayList<>();
            for (RedisMessageDto redisMsg : redisMessages) {
                Message message = convertToSpringAiMessage(redisMsg);
                if (message != null) {
                    messages.add(message);
                }
            }

            log.debug("[ChatHistory] 조회 완료 - conversationId: {}, 메시지 수: {}", conversationId, messages.size());

            return messages;

        // 조회 실패를 빈 리스트로 흡수 -> 히스토리 누락이 LLM 응답 자체를 막지는 않게 함, 원인은 로그로 남긴다
        } catch (NumberFormatException e) {
            log.error("[ChatHistory] 잘못된 conversationId 형식 - conversationId: {}", conversationId, e);
            return List.of();
        } catch (Exception e) {
            log.error("[ChatHistory] 조회 실패 - conversationId: {}", conversationId, e);
            return List.of();
        }
    }

    // RedisMessageDto 를 Spring AI Message 로 변환
    private Message convertToSpringAiMessage(RedisMessageDto redisMessage) {
        return toMessage(redisMessage.chatRole(), redisMessage.message());
    }

    // 요약 소비자가 DB 에 저장된 메시지를 변환할 때도 같은 매핑을 쓴다 -> 역할 매핑이 두 곳으로 갈라지지 않게 한다
    public static Message toMessage(ChatRole role, String text) {
        if (role == ChatRole.USER) {
            return new UserMessage(text);
        } else if (role == ChatRole.LLM) {
            return new AssistantMessage(text);
        } else {
            log.warn("[ChatHistory] 알 수 없는 ChatRole - role: {}", role);
            return null;
        }
    }
}
