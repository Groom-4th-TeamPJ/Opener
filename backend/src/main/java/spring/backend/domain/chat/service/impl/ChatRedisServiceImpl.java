package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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

/**
 * Redis Cluster 기반 채팅 서비스 구현
 *
 * <p>Hash Tag를 사용하여 세션 관련 키들이 같은 슬롯에 배치되도록 합니다.
 * 예: chat:{sessionId}:session, chat:{sessionId}:messages</p>
 *
 * <p>Hash Tag 패턴: {sessionId}가 해시 계산에 사용됨</p>
 */
@Slf4j
@Service
public class ChatRedisServiceImpl implements ChatRedisService {

    // {sessionId} 중괄호 = Redis Hash Tag -> 한 세션의 session/messages 키가 같은 슬롯에 모여
    // 클러스터에서도 멀티키 연산이 CROSSSLOT 오류 없이 동작
    private static final String SESSION_KEY_FORMAT = "chat:{%d}:session";
    private static final String MESSAGE_KEY_FORMAT = "chat:{%d}:messages";

    // TTL 1시간 -> 버려진 세션이 메모리에 영원히 남지 않게 자동 만료, SSE 타임아웃과 동일하게 맞춤
    private static final Duration SESSION_TTL = Duration.ofHours(1);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public ChatRedisServiceImpl(
            ObjectMapper objectMapper,
            // @Qualifier -> @Primary 인 Auth Redis 가 아닌 chat 클러스터 템플릿을 명시적으로 주입
            @Qualifier("chatRedisTemplate") StringRedisTemplate redisTemplate
    ) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 세션 키 생성 (Hash Tag 포함)
     * 예: chat:{123}:session
     */
    private String getSessionKey(Long sessionId) {
        return String.format(SESSION_KEY_FORMAT, sessionId);
    }

    /**
     * 메시지 키 생성 (Hash Tag 포함)
     * 예: chat:{123}:messages
     */
    private String getMessageKey(Long sessionId) {
        return String.format(MESSAGE_KEY_FORMAT, sessionId);
    }

    @Override
    public void initializeSession(Long sessionId, UUID userId) {
        String sessionKey = getSessionKey(sessionId);

        // 존재 확인 후 생성 -> 재연결 시 기존 세션 메타데이터/TTL 을 덮어쓰지 않도록 보호
        Boolean sessionExists = redisTemplate.hasKey(sessionKey);

        if (Boolean.TRUE.equals(sessionExists)) {
            log.debug("Session already exists: {}", sessionId);
            return;
        }

        try {
            // 세션 메타데이터 저장 (HASH)
            redisTemplate.opsForHash().put(sessionKey, "userId", userId.toString());
            redisTemplate.opsForHash().put(sessionKey, "createdAt", String.valueOf(System.currentTimeMillis()));

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

    // @CircuitBreaker -> Redis 장애가 길어지면 호출을 즉시 차단, 스레드가 타임아웃 대기에 묶여 전체 지연되는 것 방지
    @Override
    @CircuitBreaker(name = "redis-chat", fallbackMethod = "saveMessageFallback")
    public void saveMessage(Long sessionId, RedisMessageDto message) {
        String messageKey = getMessageKey(sessionId);
        String sessionKey = getSessionKey(sessionId);

        try {
            // 메시지를 JSON 문자열로 변환
            String json = objectMapper.writeValueAsString(message);

            // List 자료구조 + rightPush -> 대화는 시간 순서가 핵심이라 append 로 순서 보존
            redisTemplate.opsForList().rightPush(messageKey, json);

            // TTL 설정 및 검증
            Boolean ttlSet = redisTemplate.expire(messageKey, SESSION_TTL);
            if (!Boolean.TRUE.equals(ttlSet)) {
                log.warn("Failed to set TTL for messages: {}", sessionId);
            }

            // 메시지마다 세션 TTL 갱신 -> 대화 중인 활성 세션이 중간에 만료되는 것 방지
            redisTemplate.expire(sessionKey, SESSION_TTL);

            log.debug("Message saved: sessionId={}, role={}", sessionId, message.chatRole());

        } catch (Exception e) {
            log.error("Failed to save message for session: {}", sessionId, e);
            throw new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        }
    }

    @Override
    public List<RedisMessageDto> getSessionMessages(Long sessionId) {
        String messageKey = getMessageKey(sessionId);

        try {
            log.debug("[Redis Cluster] 세션 메시지 조회 시작 - sessionId: {}, key: {}", sessionId, messageKey);

            // Redis List에서 모든 메시지 조회 (0부터 -1까지 = 전체)
            List<String> jsonMessages = redisTemplate.opsForList().range(messageKey, 0, -1);

            if (jsonMessages == null || jsonMessages.isEmpty()) {
                log.debug("[Redis Cluster] 세션 메시지 없음 - sessionId: {}", sessionId);
                return List.of();
            }

            log.debug("[Redis Cluster] 세션 메시지 조회 성공 - sessionId: {}, 메시지 수: {}", sessionId, jsonMessages.size());

            // JSON 문자열을 MessageDto로 변환
            return jsonMessages.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, RedisMessageDto.class);
                        } catch (Exception e) {
                            log.error("[Redis Cluster] JSON 역직렬화 실패 - sessionId: {}, json: {}", sessionId, json, e);
                            return null;
                        }
                    })
                    .filter(msg -> msg != null)
                    .toList();

        } catch (Exception e) {
            log.error("[Redis Cluster] 세션 메시지 조회 중 예외 발생 - sessionId: {}, key: {}", sessionId, messageKey, e);
            throw new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        }
    }

    @Override
    public void deleteSession(Long sessionId) {
        String sessionKey = getSessionKey(sessionId);
        String messageKey = getMessageKey(sessionId);

        try {
            // 세션과 메시지 모두 삭제
            redisTemplate.delete(sessionKey);
            redisTemplate.delete(messageKey);

            log.info("[Redis Cluster] 세션 삭제 완료 - sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("[Redis Cluster] 세션 삭제 실패 - sessionId: {}", sessionId, e);
        }
    }

    @Override
    public void deleteMessage(Long sessionId) {
        String messageKey = getMessageKey(sessionId);

        try {
            redisTemplate.delete(messageKey);
            log.info("[Redis Cluster] 메시지 삭제 완료 - sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("[Redis Cluster] 메시지 삭제 실패 - sessionId: {}", sessionId, e);
        }
    }

    /**
     * 세션 소유자 검증
     *
     * @param sessionId 세션 ID
     * @param userId    사용자 ID
     * @throws BusinessException 권한이 없거나 세션이 존재하지 않는 경우
     */
    // 소유자 검증 -> sessionId 만 알면 남의 대화 접근 가능하므로 Redis 저장 userId 와 대조해 차단
    @Override
    @CircuitBreaker(name = "redis-chat", fallbackMethod = "validateSessionOwnerFallback")
    public void validateSessionOwner(Long sessionId, UUID userId) {
        String sessionKey = getSessionKey(sessionId);

        try {
            String storedUserId = (String) redisTemplate.opsForHash().get(sessionKey, "userId");

            if (storedUserId == null) {
                log.warn("[Redis Cluster] 세션 권한 검증 실패 - 세션이 존재하지 않음 - sessionId: {}, userId: {}",
                        sessionId, userId);
                throw new BusinessException(ErrorCode.INVALID_SESSION);
            }

            if (!storedUserId.equals(userId.toString())) {
                log.warn("[Redis Cluster] 세션 권한 검증 실패 - 사용자 불일치 - sessionId: {}, expectedUserId: {}, actualUserId: {}",
                        sessionId, storedUserId, userId);
                throw new BusinessException(ErrorCode.INVALID_SESSION);
            }

            log.debug("[Redis Cluster] 세션 권한 검증 성공 - sessionId: {}, userId: {}", sessionId, userId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[Redis Cluster] 세션 권한 검증 중 예외 발생 - sessionId: {}, userId: {}", sessionId, userId, e);
            throw new BusinessException(ErrorCode.INVALID_SESSION);
        }
    }

    // fallback 2개로 분리 -> Circuit OPEN(차단 상태)과 일반 I/O 실패를 다른 에러코드로 구분 응답
    // Circuit OPEN 상태 — Redis 장애 지속 중이므로 즉시 차단
    @SuppressWarnings("unused")
    private void saveMessageFallback(Long sessionId, RedisMessageDto message, CallNotPermittedException ex) {
        log.warn("[Redis Cluster] Circuit OPEN - 메시지 저장 차단 - sessionId: {}", sessionId);
        throw new BusinessException(ErrorCode.REDIS_CIRCUIT_OPEN);
    }

    // 일반 Redis I/O 예외 — CB 실패 카운트에 반영됨
    @SuppressWarnings("unused")
    private void saveMessageFallback(Long sessionId, RedisMessageDto message, Throwable t) {
        if (t instanceof BusinessException be) {
            throw be;
        }
        log.error("[Redis Cluster] 메시지 저장 실패(CB 카운트됨) - sessionId: {}, cause: {}",
                sessionId, t.getMessage());
        throw new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
    }

    @SuppressWarnings("unused")
    private void validateSessionOwnerFallback(Long sessionId, UUID userId, CallNotPermittedException ex) {
        log.warn("[Redis Cluster] Circuit OPEN - 세션 권한 검증 차단 - sessionId: {}", sessionId);
        throw new BusinessException(ErrorCode.REDIS_CIRCUIT_OPEN);
    }

    @SuppressWarnings("unused")
    private void validateSessionOwnerFallback(Long sessionId, UUID userId, Throwable t) {
        if (t instanceof BusinessException be) {
            throw be;
        }
        log.error("[Redis Cluster] 세션 권한 검증 실패(CB 카운트됨) - sessionId: {}, cause: {}",
                sessionId, t.getMessage());
        throw new BusinessException(ErrorCode.INVALID_SESSION);
    }
}
