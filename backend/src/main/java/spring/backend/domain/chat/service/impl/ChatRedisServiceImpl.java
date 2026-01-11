package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import spring.backend.domain.chat.dto.redis_dto.MessageInputDto;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRedisServiceImpl implements ChatRedisService {

    private static final String SESSION_KEY_PREFIX = "chat:session:";
    private static final Duration SESSION_TTL = Duration.ofMinutes(5);
    private final ObjectMapper objectMapper;

    @Qualifier("chatRedisTemplate")
    private final StringRedisTemplate redisTemplate;

    @Override
    public void initializeSession(Long sessionId, UUID userId) {

        String sessionKey = SESSION_KEY_PREFIX + sessionId;

        // 중복 초기화 방지
        Boolean sessionExists = redisTemplate.hasKey(sessionKey);

        if (Boolean.TRUE.equals(sessionExists)) {
            return;
        }

        try {
            // 세션 메타데이터 저장 (HASH)
            redisTemplate.opsForHash().put(sessionKey, "userId", userId.toString());

            // TTL 설정
            redisTemplate.expire(sessionKey, SESSION_TTL);

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SESSION_INITIALIZE_FAIL);
        }
    }

    @Override
    public void saveMessage(Long sessionId, MessageInputDto message) {

        // 레디스 접근 키 (message)
        String key = SESSION_KEY_PREFIX + sessionId + ":messages";

        try {
            // 메시지를 JSON 문자열로 변환
            String json = objectMapper.writeValueAsString(message);

            // Redis List에 메시지 추가 (순서 보장)
            redisTemplate.opsForList().rightPush(key, json);

            // TTL 설정
            redisTemplate.expire(key, SESSION_TTL);

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        }
    }

    @Override
    public List<ChatMessageResponse> getSessionMessages(Long sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;

        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {

                return new ArrayList<>();
            }

            Map<String, Object> sessionData =
                    objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
                    });
            return objectMapper.convertValue(
                    sessionData.get("messages"), new TypeReference<List<ChatMessageResponse>>() {
                    });

        } catch (Exception e) {

            return new ArrayList<>();
        }
    }

    @Override
    public void deleteSession(Long sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        redisTemplate.delete(sessionKey);

        String messagesKey = sessionKey + ":messages";
        redisTemplate.delete(messagesKey);
    }

    // 세션 주인 확인 (권한 없으면 예외 throw)
    @Override
    public void validateSessionOwner(Long sessionId, UUID userId) {

        if (redisTemplate.opsForHash().get(SESSION_KEY_PREFIX + sessionId, "userId") != userId.toString()) {
            throw new BusinessException(ErrorCode.INVALID_SESSION);
        }
    }
}
