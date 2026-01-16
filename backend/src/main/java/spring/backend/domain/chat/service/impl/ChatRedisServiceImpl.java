package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@Slf4j
@Service
public class ChatRedisServiceImpl implements ChatRedisService {

    private static final String SESSION_KEY_PREFIX = "chat:session:";
    // 채팅 세션 TTL: 5분 (SSE 타임아웃과 동일, 메시지 송수신 시 자동 갱신)
    private static final Duration SESSION_TTL = Duration.ofMinutes(5);
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public ChatRedisServiceImpl(
            ObjectMapper objectMapper,
            @Qualifier("chatRedisTemplate") StringRedisTemplate redisTemplate
    ) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

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

            // TTL 설정 및 검증
            Boolean ttlSet = redisTemplate.expire(sessionKey, SESSION_TTL);
            if (!Boolean.TRUE.equals(ttlSet)) {
                log.warn("Failed to set TTL for session: {}", sessionId);
            } else {
                log.debug("Session initialized successfully: {} with TTL: {}", sessionId, SESSION_TTL);
            }

        } catch (Exception e) {
            log.error("Failed to initialize session: {}", sessionId, e);
            throw new BusinessException(ErrorCode.SESSION_INITIALIZE_FAIL);
        }
    }

    @Override
    public void saveMessage(Long sessionId, RedisMessageDto message) {

        // 레디스 접근 키 (message)
        String messageKey = SESSION_KEY_PREFIX + sessionId + ":messages";

        try {
            // 메시지를 JSON 문자열로 변환
            String json = objectMapper.writeValueAsString(message);

            // Redis List에 메시지 추가 (순서 보장)
            redisTemplate.opsForList().rightPush(messageKey, json);

            // TTL 설정 및 검증
            Boolean ttlSet = redisTemplate.expire(messageKey, SESSION_TTL);
            if (!Boolean.TRUE.equals(ttlSet)) {
                log.warn("Failed to set TTL for messages: {}", sessionId);
            }

            // 세션 키의 TTL도 갱신 (메시지 저장 시 세션도 연장)
            String sessionKey = SESSION_KEY_PREFIX + sessionId;
            redisTemplate.expire(sessionKey, SESSION_TTL);

        } catch (Exception e) {
            log.error("Failed to save message for session: {}", sessionId, e);
            throw new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        }
    }

    @Override
    public List<RedisMessageDto> getSessionMessages(Long sessionId) {
        String messageKey = SESSION_KEY_PREFIX + sessionId + ":messages";

        try {
            log.debug("[Redis] 세션 메시지 조회 시작 - sessionId: {}, key: {}", sessionId, messageKey);

            // Redis List에서 모든 메시지 조회 (0부터 -1까지 = 전체)
            // 가져올때는 json 포멧
            List<String> jsonMessages = redisTemplate.opsForList().range(messageKey, 0, -1);

            if (jsonMessages == null || jsonMessages.isEmpty()) {
                log.debug("[Redis] 세션 메시지 없음 - sessionId: {}", sessionId);
                return List.of();
            }

            log.debug("[Redis] 세션 메시지 조회 성공 - sessionId: {}, 메시지 수: {}", sessionId, jsonMessages.size());

            // JSON 문자열을 MessageDto로 변환
            return jsonMessages.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, RedisMessageDto.class);
                        } catch (Exception e) {
                            log.error("[Redis] JSON 역직렬화 실패 - sessionId: {}, json: {}", sessionId, json, e);
                            return null;
                        }
                    })
                    .filter(msg -> msg != null)
                    .toList();

        } catch (Exception e) {
            log.error("[Redis] 세션 메시지 조회 중 예외 발생 - sessionId: {}, key: {}", sessionId, messageKey, e);
            throw new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        }
    }

    @Override
    public void deleteSession(Long sessionId) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        redisTemplate.delete(sessionKey);

        String messagesKey = sessionKey + ":messages";
        redisTemplate.delete(messagesKey);
    }

    @Override
    public void deleteMessage(Long sessionId) {
        String messageKey = SESSION_KEY_PREFIX + sessionId + ":messages";
        redisTemplate.delete(messageKey);
    }

    // 세션 주인 확인 (권한 없으면 예외 throw)
    @Override
    public void validateSessionOwner(Long sessionId, UUID userId) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;

        try {
            String storedUserId = (String) redisTemplate.opsForHash().get(sessionKey, "userId");

            if (storedUserId == null) {
                log.warn("[Redis] 세션 권한 검증 실패 - 세션이 존재하지 않음 - sessionId: {}, userId: {}",
                        sessionId, userId);
                throw new BusinessException(ErrorCode.INVALID_SESSION);
            }

            if (!storedUserId.equals(userId.toString())) {
                log.warn("[Redis] 세션 권한 검증 실패 - 사용자 불일치 - sessionId: {}, expectedUserId: {}, actualUserId: {}",
                        sessionId, storedUserId, userId);
                throw new BusinessException(ErrorCode.INVALID_SESSION);
            }

            log.debug("[Redis] 세션 권한 검증 성공 - sessionId: {}, userId: {}", sessionId, userId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Redis] 세션 권한 검증 중 예외 발생 - sessionId: {}, userId: {}", sessionId, userId, e);
            throw new BusinessException(ErrorCode.INVALID_SESSION);
        }
    }
}
