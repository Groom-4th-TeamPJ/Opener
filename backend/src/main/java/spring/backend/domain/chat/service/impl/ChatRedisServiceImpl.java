package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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

    // LLM 컨텍스트로 넘길 최근 메시지 개수 -> 하드코딩하면 모델/비용 정책이 바뀔 때 재배포가 필요하므로 설정값으로 분리
    private final int historyWindow;

    private final MeterRegistry meterRegistry;

    public ChatRedisServiceImpl(
            ObjectMapper objectMapper,
            // @Qualifier -> @Primary 인 Auth Redis 가 아닌 chat 클러스터 템플릿을 명시적으로 주입
            @Qualifier("chatRedisTemplate") StringRedisTemplate redisTemplate,
            @Value("${app.chat.history-window:20}") int historyWindow,
            MeterRegistry meterRegistry
    ) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.historyWindow = historyWindow;
        this.meterRegistry = meterRegistry;
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
            // 기존 키 재사용 전 소유자 대조 -> 검증 없이 return 하면 TTL 이 남은 남의 세션에 그대로 올라타게 됨
            // 호출부 분기가 아니라 여기서 막는 이유 -> 분기가 늘어도 방어선이 새지 않음
            // validateSessionOwner 를 직접 부르지 않고 헬퍼를 쓰는 이유 -> 같은 클래스 내부 호출은
            // Spring AOP 프록시를 우회해 @CircuitBreaker 가 적용되지 않는다
            assertSessionOwner(sessionId, userId, "세션 재사용");

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

        // 직렬화 실패만 도메인 오류 -> Redis I/O 실패는 그대로 던져 서킷이 세게 한다
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message for session: {}", sessionId, e);
            throw new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        }
    }

    // 영속화 경로 전용 -> 대화가 잘리면 DB 에 반쪽 기록이 남으므로 전량 반환 유지
    @Override
    public List<RedisMessageDto> getSessionMessages(Long sessionId) {
        return fetchMessages(sessionId, 0, -1);
    }

    // LLM 컨텍스트 전용 -> 턴이 쌓일수록 프롬프트가 선형 증가해 비용·TTFT 가 나빠지므로 최근 N 개로 상한
    @Override
    public List<RedisMessageDto> getRecentSessionMessages(Long sessionId) {
        // 음수 인덱스 = 뒤에서부터 -> 저장 개수가 N 보다 적어도 Redis 가 있는 만큼만 반환해 별도 길이 체크 불필요
        // 설정값이 0 이하면 윈도우 비활성으로 보고 전체 반환
        long start = historyWindow > 0 ? -historyWindow : 0;
        return fetchMessages(sessionId, start, -1);
    }

    // 조회 범위만 다르고 역직렬화/예외 처리는 동일 -> 중복 대신 범위를 인자로 받아 한 곳에서 처리
    private List<RedisMessageDto> fetchMessages(Long sessionId, long start, long end) {
        String messageKey = getMessageKey(sessionId);

        try {
            log.debug("[Redis Cluster] 세션 메시지 조회 시작 - sessionId: {}, key: {}, range: [{}, {}]",
                    sessionId, messageKey, start, end);

            List<String> jsonMessages = redisTemplate.opsForList().range(messageKey, start, end);

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
        // DataAccessException 은 감싸지 않고 전파 -> 서킷이 인프라 장애만 실패로 세게 한다
        // 여기서 BusinessException 으로 감싸면 만료 세션과 노드 다운이 같은 타입이 되어 분류가 불가능해진다
        assertSessionOwner(sessionId, userId, "세션 권한 검증");
        log.debug("[Redis Cluster] 세션 권한 검증 성공 - sessionId: {}, userId: {}", sessionId, userId);
    }

    // 소유자 대조 단일 구현 -> initializeSession(신규 연결)과 validateSessionOwner(기존 연결 재사용)가
    // 같은 로직을 각각 들고 있으면 판정 조건을 바꿀 때 한쪽만 고쳐 규칙이 갈라진다
    private void assertSessionOwner(Long sessionId, UUID userId, String context) {
        String storedUserId = (String) redisTemplate.opsForHash().get(getSessionKey(sessionId), "userId");

        if (storedUserId == null) {
            log.warn("[Redis Cluster] {} 실패 - 세션이 존재하지 않음 - sessionId: {}, userId: {}",
                    context, sessionId, userId);
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }

        if (!storedUserId.equals(userId.toString())) {
            log.warn("[Redis Cluster] {} 실패 - 소유자 불일치 - sessionId: {}, storedUserId: {}, requestUserId: {}",
                    context, sessionId, storedUserId, userId);
            throw new BusinessException(ErrorCode.SESSION_ACCESS_DENIED);
        }
    }

    // 저장 실패는 스트리밍을 끊을 사유가 아니다 -> 이번 턴 프롬프트는 ChatPromptAssembler 가 질문을 보장한다
    // 버려진 사실을 메트릭으로 남겨 조용한 유실이 되지 않게 한다
    @SuppressWarnings("unused")
    private void saveMessageFallback(Long sessionId, RedisMessageDto message, CallNotPermittedException ex) {
        log.warn("[Redis Cluster] Circuit OPEN - 메시지 저장 생략 - sessionId: {}", sessionId);
        meterRegistry.counter("chat.redis.save.dropped", "reason", "circuit_open").increment();
    }

    @SuppressWarnings("unused")
    private void saveMessageFallback(Long sessionId, RedisMessageDto message, Throwable t) {
        if (t instanceof BusinessException be) {
            throw be;
        }
        log.error("[Redis Cluster] 메시지 저장 실패(CB 카운트됨) - sessionId: {}, cause: {}",
                sessionId, t.getMessage());
        meterRegistry.counter("chat.redis.save.dropped", "reason", "io_error").increment();
    }

    // 소유자 검증은 인가 게이트라 fail-close 유지 -> 못 읽는 상태에서 통과시키면 그 자체가 인가 우회
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
        throw new BusinessException(ErrorCode.REDIS_CIRCUIT_OPEN);
    }

    // private fallback 을 단위 테스트에서 직접 부르기 위한 위임
    void saveMessageFallbackForTest(Long sessionId, RedisMessageDto message, Throwable t) {
        saveMessageFallback(sessionId, message, t);
    }
}
