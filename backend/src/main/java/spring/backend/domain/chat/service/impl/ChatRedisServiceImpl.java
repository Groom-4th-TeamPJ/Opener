package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
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

    // TTL 1시간 -> 버려진 세션이 메모리에 영원히 남지 않게 자동 만료
    // SSE/async 타임아웃(30분)과는 다른 값이다
    private static final Duration SESSION_TTL = Duration.ofHours(1);

    // 세션 선점 스크립트 -> 소유자 기록(HSETNX)과 TTL(EXPIRE)이 한 왕복
    private static final RedisScript<Long> SESSION_CLAIM_SCRIPT = loadSessionClaimScript();

    // 저장 스크립트 -> RPUSH 와 TTL 갱신 2회가 한 왕복, 반환값이 push 직후 목록 길이
    private static final RedisScript<Long> MESSAGE_SAVE_SCRIPT = loadScript("scripts/chat-message-save.lua");

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    // LLM 컨텍스트로 넘길 최근 메시지 개수 -> 하드코딩하면 모델/비용 정책이 바뀔 때 재배포가 필요하므로 설정값으로 분리
    private final int historyWindow;

    private final MeterRegistry meterRegistry;

    // 복제 지연을 의심하는 재조회와 쓰기 직후 확인 전용 (ReadFrom.MASTER)
    private final StringRedisTemplate masterRedisTemplate;

    public ChatRedisServiceImpl(
            ObjectMapper objectMapper,
            // @Qualifier -> @Primary 인 Auth Redis 가 아닌 chat 클러스터 템플릿을 명시적으로 주입
            @Qualifier("chatRedisTemplate") StringRedisTemplate redisTemplate,
            @Value("${app.chat.history-window:20}") int historyWindow,
            MeterRegistry meterRegistry,
            @Qualifier("chatRedisMasterTemplate") StringRedisTemplate masterRedisTemplate
    ) {
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.historyWindow = historyWindow;
        this.meterRegistry = meterRegistry;
        this.masterRedisTemplate = masterRedisTemplate;
    }

    private static RedisScript<Long> loadSessionClaimScript() {
        return loadScript("scripts/chat-session-claim.lua");
    }

    private static RedisScript<Long> loadScript(String path) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(Long.class);
        return script;
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

    // 소유자 기록과 TTL 을 마스터에서 스크립트 한 번으로 처리
    // 복제본 hasKey + HSET + EXPIRE 로 나누면 (1) 복제 지연 창에서 동시 connect 2건이 모두 "세션 없음" 을 보고 나중 쪽이 소유자가 되고
    // (2) HSET 과 EXPIRE 사이에서 끊기면 만료 없는 키가 남는다
    // DataAccessException 은 감싸지 않고 전파 -> 서킷이 인프라 장애만 실패로 세게 한다
    @Override
    @CircuitBreaker(name = "redis-chat", fallbackMethod = "initializeSessionFallback")
    public void initializeSession(Long sessionId, UUID userId) {
        String sessionKey = getSessionKey(sessionId);

        Long claimed = masterRedisTemplate.execute(
                SESSION_CLAIM_SCRIPT,
                List.of(sessionKey),
                userId.toString(),
                String.valueOf(SESSION_TTL.toSeconds()),
                String.valueOf(System.currentTimeMillis()));

        if (claimed == null) {
            log.error("[Redis Cluster] 세션 선점 결과 없음 - sessionId: {}", sessionId);
            throw new BusinessException(ErrorCode.SESSION_INITIALIZE_FAIL);
        }
        // 키는 있는데 userId 가 없으면 소유자를 확인할 수 없다 -> 재사용 검증과 같은 SESSION_NOT_FOUND 로 거부
        if (claimed == 2L) {
            log.warn("[Redis Cluster] 세션 선점 실패 - 소유자 기록이 없는 키 - sessionId: {}, userId: {}",
                    sessionId, userId);
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }
        if (claimed == 0L) {
            log.warn("[Redis Cluster] 세션 선점 실패 - 소유자 불일치 - sessionId: {}, requestUserId: {}",
                    sessionId, userId);
            throw new BusinessException(ErrorCode.SESSION_ACCESS_DENIED);
        }
        log.debug("Session claimed: {} with TTL: {}", sessionId, SESSION_TTL);
    }

    // @CircuitBreaker -> Redis 장애가 길어지면 호출을 즉시 차단, 스레드가 타임아웃 대기에 묶여 전체 지연되는 것 방지
    // 반환 = push 직후 목록 길이 -> 호출자가 이 질문 직전까지만 복제본에서 읽는 범위를 계산한다
    @Override
    @CircuitBreaker(name = "redis-chat", fallbackMethod = "saveMessageFallback")
    public long saveMessage(Long sessionId, RedisMessageDto message) {
        String messageKey = getMessageKey(sessionId);
        String sessionKey = getSessionKey(sessionId);

        try {
            // 메시지를 JSON 문자열로 변환
            String json = objectMapper.writeValueAsString(message);

            // 스크립트 한 번 -> rightPush(대화는 시간 순서가 핵심이라 append) + 메시지 TTL + 세션 TTL 이 한 왕복
            // 메시지마다 세션 TTL 을 갱신해 대화 중인 활성 세션이 중간에 만료되는 것을 막는다
            // 스크립트는 쓰기 명령이라 읽기 분산 설정과 무관하게 마스터에서 실행된다
            Long length = redisTemplate.execute(
                    MESSAGE_SAVE_SCRIPT,
                    List.of(messageKey, sessionKey),
                    json,
                    String.valueOf(SESSION_TTL.toSeconds()));

            log.debug("Message saved: sessionId={}, role={}, length={}", sessionId, message.chatRole(), length);

            // 결과가 없으면 길이를 알 수 없다 -> 저장이 버려진 턴과 같은 -1 로 마스터 최근 읽기를 타게 한다
            return length == null ? -1L : length;

        // 직렬화 실패만 도메인 오류 -> Redis I/O 실패는 그대로 던져 서킷이 세게 한다
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message for session: {}", sessionId, e);
            throw new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        }
    }

    // 영속화 경로 전용 -> 대화가 잘리면 DB 에 반쪽 기록이 남으므로 전량 반환 유지
    @Override
    public List<RedisMessageDto> getSessionMessages(Long sessionId) {
        // 영속화는 스트리밍 종료 후 별도 요청이라 복제 지연 창이 닫혀 있다 -> replica 유지
        return meterRegistry.timer("chat.redis.read", "route", "replica")
                .record(() -> fetchMessages(redisTemplate, sessionId, 0, -1));
    }

    // LLM 컨텍스트 전용 -> 턴이 쌓일수록 프롬프트가 선형 증가해 비용·TTFT 가 나빠지므로 최근 N 개로 상한
    // 현재 질문은 읽지 않는다 -> 요청에서 받은 원문을 프롬프트 조립이 붙이므로 방금 쓴 항목을 읽을 이유가 없다
    // 그래서 쓰기 직후 읽기인데도 복제본으로 보낼 수 있고, 복제본이 덜 따라왔는지는 기대 항목 수로 알아낸다
    @Override
    public List<RedisMessageDto> getHistoryBefore(Long sessionId, long writtenLength) {
        // 윈도우 W 는 질문 포함 개수 -> 이전 메시지는 W-1 개, 설정값이 0 이하면 윈도우 비활성으로 보고 전체
        if (writtenLength < 0) {
            // 저장이 버려진 턴 -> 길이를 모르므로 마스터의 마지막 W-1 개를 읽는다 (질문이 저장되지 않았으니 이전 대화뿐이다)
            // W=1 이면 -(W-1) 이 0 이 되어 "처음부터" 로 읽히므로 읽지 않는다
            if (historyWindow == 1) {
                return List.of();
            }
            long start = historyWindow > 0 ? -(historyWindow - 1L) : 0;
            return withoutPartial(timedFetch("master", masterRedisTemplate, sessionId, start, -1));
        }

        // 질문(마지막 항목) 직전까지의 절대 인덱스 [from, L-2] -> 음수 인덱스는 그 사이 쓰기가 끼면 한 칸씩 밀린다
        long from = historyWindow > 0 ? Math.max(0, writtenLength - historyWindow) : 0;
        long to = writtenLength - 2;
        long expected = to - from + 1;
        if (expected <= 0) {
            return List.of();
        }

        List<String> raw = timedRange("replica", redisTemplate, sessionId, from, to);
        // 기대보다 짧다 = 복제본이 방금 쓴 항목까지 못 따라왔다 -> 마스터에서 같은 범위를 한 번만 다시 읽는다
        // 짧은 결과를 그대로 쓰면 직전 LLM 답이 프롬프트에서 빠진다
        if (raw.size() < expected) {
            meterRegistry.counter("chat.redis.history.fallback").increment();
            raw = timedRange("master-fallback", masterRedisTemplate, sessionId, from, to);
        }
        // 길이는 partial 을 제외하기 전 항목 수로 판정했다 -> partial 도 정상적으로 저장된 한 항목이다
        return withoutPartial(parseMessages(sessionId, raw));
    }

    // partial 표식 메시지는 프롬프트에서 뺀다 -> 끊긴 문장을 모델이 자기 답으로 이어 쓰지 않게 한다
    // 영속화 읽기(getSessionMessages)는 그대로 둬 소비자가 표식을 보고 직접 제외한다
    private List<RedisMessageDto> withoutPartial(List<RedisMessageDto> messages) {
        return messages.stream().filter(message -> !message.partial()).toList();
    }

    private List<RedisMessageDto> timedFetch(
            String route, StringRedisTemplate template, Long sessionId, long start, long end) {
        return parseMessages(sessionId, timedRange(route, template, sessionId, start, end));
    }

    private List<String> timedRange(
            String route, StringRedisTemplate template, Long sessionId, long start, long end) {
        return meterRegistry.timer("chat.redis.read", "route", route)
                .record(() -> readRange(template, sessionId, start, end));
    }

    // 조회 범위만 다르고 역직렬화/예외 처리는 동일 -> 중복 대신 범위를 인자로 받아 한 곳에서 처리
    private List<RedisMessageDto> fetchMessages(
            StringRedisTemplate template, Long sessionId, long start, long end) {
        return parseMessages(sessionId, readRange(template, sessionId, start, end));
    }

    // 원문 그대로 읽는다 -> 복제 지연 판정이 역직렬화로 걸러진 개수가 아니라 Redis 가 돌려준 개수를 봐야 한다
    private List<String> readRange(StringRedisTemplate template, Long sessionId, long start, long end) {
        String messageKey = getMessageKey(sessionId);

        try {
            log.debug("[Redis Cluster] 세션 메시지 조회 시작 - sessionId: {}, key: {}, range: [{}, {}]",
                    sessionId, messageKey, start, end);

            List<String> jsonMessages = template.opsForList().range(messageKey, start, end);

            if (jsonMessages == null || jsonMessages.isEmpty()) {
                log.debug("[Redis Cluster] 세션 메시지 없음 - sessionId: {}", sessionId);
                return List.of();
            }

            log.debug("[Redis Cluster] 세션 메시지 조회 성공 - sessionId: {}, 메시지 수: {}", sessionId, jsonMessages.size());
            return jsonMessages;

        } catch (Exception e) {
            log.error("[Redis Cluster] 세션 메시지 조회 중 예외 발생 - sessionId: {}, key: {}", sessionId, messageKey, e);
            throw new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        }
    }

    // JSON 문자열을 MessageDto로 변환
    private List<RedisMessageDto> parseMessages(Long sessionId, List<String> jsonMessages) {
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

    // 방금 저장하려던 USER 메시지를 되돌린다 -> 마지막 항목이 그 메시지일 때만 꺼낸다
    // 무조건 꺼내면 그 저장이 saveMessageFallback 에서 버려졌을 때 앞 턴의 LLM 답이 지워진다
    // 목록 전체를 지워도 같은 세션의 앞선 대화가 사라진다
    @Override
    public void removeLastMessage(Long sessionId, RedisMessageDto expected) {
        String messageKey = getMessageKey(sessionId);

        try {
            // saveMessage 직후에 도는 경로 -> replica 는 복제 지연 창에서 방금 쓴 항목 대신 앞 턴의 꼬리를 돌려준다
            String lastJson = masterRedisTemplate.opsForList().index(messageKey, -1);
            if (lastJson == null) {
                log.warn("[Redis Cluster] 마지막 메시지 제거 생략 - 목록이 비어 있음 - sessionId: {}", sessionId);
                return;
            }
            RedisMessageDto last = objectMapper.readValue(lastJson, RedisMessageDto.class);
            if (last.chatRole() != expected.chatRole() || !Objects.equals(last.message(), expected.message())) {
                log.warn("[Redis Cluster] 마지막 메시지 제거 생략 - 마지막 항목이 되돌릴 메시지가 아님 - sessionId: {}, lastRole: {}",
                        sessionId, last.chatRole());
                return;
            }
            masterRedisTemplate.opsForList().rightPop(messageKey);
            log.debug("[Redis Cluster] 마지막 메시지 제거 - sessionId: {}", sessionId);
        } catch (Exception e) {
            // 제거 실패는 이미 시작한 정리를 막을 사유가 아니다 -> 남는 메시지는 소비자의 답 없는 턴 제외가 거른다
            log.error("[Redis Cluster] 마지막 메시지 제거 실패 - sessionId: {}", sessionId, e);
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
        String sessionKey = getSessionKey(sessionId);

        String storedUserId = (String) meterRegistry.timer("chat.redis.read", "route", "replica")
                .record(() -> redisTemplate.opsForHash().get(sessionKey, "userId"));

        // replica 가 비었을 때만 master 재확인 -> 정상 경로는 그대로 분산되고 복제 지연 의심 순간만 선형화
        // 무조건 master 로 읽으면 분산이 사라지고, 재확인이 없으면 방금 만든 세션이 없는 것으로 보인다
        if (storedUserId == null) {
            storedUserId = (String) meterRegistry.timer("chat.redis.read", "route", "master")
                    .record(() -> masterRedisTemplate.opsForHash().get(sessionKey, "userId"));
        }

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
    // 버려진 저장은 -1 -> 호출자가 길이를 모르는 턴으로 보고 마스터에서 최근 W-1 개를 읽는다
    @SuppressWarnings("unused")
    private long saveMessageFallback(Long sessionId, RedisMessageDto message, CallNotPermittedException ex) {
        log.warn("[Redis Cluster] Circuit OPEN - 메시지 저장 생략 - sessionId: {}", sessionId);
        meterRegistry.counter("chat.redis.save.dropped", "reason", "circuit_open").increment();
        return -1L;
    }

    @SuppressWarnings("unused")
    private long saveMessageFallback(Long sessionId, RedisMessageDto message, Throwable t) {
        if (t instanceof BusinessException be) {
            throw be;
        }
        log.error("[Redis Cluster] 메시지 저장 실패(CB 카운트됨) - sessionId: {}, cause: {}",
                sessionId, t.getMessage());
        meterRegistry.counter("chat.redis.save.dropped", "reason", "io_error").increment();
        return -1L;
    }

    // 선점도 인가 게이트라 fail-close -> 못 만든 세션을 만든 것처럼 통과시키지 않는다
    @SuppressWarnings("unused")
    private void initializeSessionFallback(Long sessionId, UUID userId, CallNotPermittedException ex) {
        log.warn("[Redis Cluster] Circuit OPEN - 세션 선점 차단 - sessionId: {}", sessionId);
        throw new BusinessException(ErrorCode.REDIS_CIRCUIT_OPEN);
    }

    @SuppressWarnings("unused")
    private void initializeSessionFallback(Long sessionId, UUID userId, Throwable t) {
        if (t instanceof BusinessException be) {
            throw be;
        }
        log.error("[Redis Cluster] 세션 선점 실패(CB 카운트됨) - sessionId: {}, cause: {}",
                sessionId, t.getMessage());
        throw new BusinessException(ErrorCode.SESSION_INITIALIZE_FAIL);
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
    long saveMessageFallbackForTest(Long sessionId, RedisMessageDto message, Throwable t) {
        return saveMessageFallback(sessionId, message, t);
    }

    long saveMessageFallbackForTest(Long sessionId, RedisMessageDto message, CallNotPermittedException ex) {
        return saveMessageFallback(sessionId, message, ex);
    }
}
