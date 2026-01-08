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
import spring.backend.domain.chat.dto.response.ChatMessageDto;
import spring.backend.domain.chat.service.spec.ChatRedisService;

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
    String messageKey = sessionKey + ":messages";

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
      throw new RuntimeException("세션 초기화 실패", e);
    }
  }

  @Override
  public void saveMessage(String sessionId, ChatMessageDto message) {
    String key = SESSION_KEY_PREFIX + sessionId;

    try {
      String json = redisTemplate.opsForValue().get(key);
      if (json == null) {
        throw new IllegalArgumentException("Session not found: " + sessionId);
      }

      Map<String, Object> sessionData =
              objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
              });
      List<ChatMessageDto> messages =
              objectMapper.convertValue(
                      sessionData.get("messages"), new TypeReference<List<ChatMessageDto>>() {
                      });

      messages.add(message);
      sessionData.put("messages", messages);

      String updatedJson = objectMapper.writeValueAsString(sessionData);
      redisTemplate.opsForValue().set(key, updatedJson, SESSION_TTL);


    } catch (Exception e) {

      throw new RuntimeException("Message save failed", e);
    }
  }

  @Override
  public List<ChatMessageDto> getSessionMessages(String sessionId) {
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
              sessionData.get("messages"), new TypeReference<List<ChatMessageDto>>() {
              });

    } catch (Exception e) {

      return new ArrayList<>();
    }
  }

  @Override
  public UUID getSessionOwnerId(String sessionId) {
    String key = SESSION_KEY_PREFIX + sessionId;

    try {
      String json = redisTemplate.opsForValue().get(key);
      if (json == null) {
        return null;
      }

      Map<String, Object> sessionData =
              objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
              });
      Object userIdObj = sessionData.get("userId");

      if (userIdObj instanceof String) {
        return UUID.fromString((String) userIdObj);
      }

      return null;

    } catch (Exception e) {

      return null;
    }
  }

  @Override
  public void deleteSession(String sessionId) {
    String key = SESSION_KEY_PREFIX + sessionId;
    redisTemplate.delete(key);

  }
}
