package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.data.redis.RedisConnectionFailureException;
import java.util.List;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// SSE Sink 하이재킹 방어선 회귀 테스트
// sessionId 만 알면 남의 세션 Sink 에 올라탈 수 있었던 결함이라 반드시 테스트를 남긴다
@ExtendWith(MockitoExtension.class)
class ChatRedisServiceImplTest {

    private static final Long SESSION_ID = 42L;
    private static final String SESSION_KEY = "chat:{42}:session";
    private static final String MESSAGE_KEY = "chat:{42}:messages";
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private StringRedisTemplate masterRedisTemplate;
    @Mock private HashOperations<String, Object, Object> masterHashOperations;
    @Mock private ListOperations<String, String> masterListOperations;
    @Mock private ListOperations<String, String> listOperations;

    private ChatRedisServiceImpl service;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new ChatRedisServiceImpl(new ObjectMapper(), redisTemplate, 20, meterRegistry, masterRedisTemplate);
    }

    @Test
    @DisplayName("기존 세션 키의 소유자가 다르면 SESSION_ACCESS_DENIED 로 재사용을 거부한다")
    void initializeSession_ownerMismatch_throw() {
        UUID owner = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();

        when(redisTemplate.hasKey(SESSION_KEY)).thenReturn(true);
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(owner.toString());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.initializeSession(SESSION_ID, attacker));

        assertEquals(ErrorCode.SESSION_ACCESS_DENIED, ex.getErrorCode());

        // 거부된 요청이 기존 세션 메타데이터/TTL 을 건드리면 안 됨
        verify(hashOperations, never()).put(anyString(), any(), any());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("세션 키에 userId 가 없으면 소유자를 확인할 수 없으므로 거부한다")
    void initializeSession_missingOwner_throw() {
        when(redisTemplate.hasKey(SESSION_KEY)).thenReturn(true);
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(null);
        doReturn(masterHashOperations).when(masterRedisTemplate).opsForHash();
        when(masterHashOperations.get(SESSION_KEY, "userId")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.initializeSession(SESSION_ID, UUID.randomUUID()));

        assertEquals(ErrorCode.SESSION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("소유자가 같으면 기존 세션을 덮어쓰지 않고 그대로 재사용한다")
    void initializeSession_sameOwner_reuse() {
        UUID owner = UUID.randomUUID();

        when(redisTemplate.hasKey(SESSION_KEY)).thenReturn(true);
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(owner.toString());

        assertDoesNotThrow(() -> service.initializeSession(SESSION_ID, owner));

        // 재연결 시 기존 TTL 을 초기화하면 대화 중 세션이 예상보다 길어지거나 짧아짐
        verify(hashOperations, never()).put(anyString(), any(), any());
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("세션 키가 없으면 소유자와 생성시각을 새로 기록한다")
    void initializeSession_newSession_writesMetadata() {
        UUID owner = UUID.randomUUID();

        when(redisTemplate.hasKey(SESSION_KEY)).thenReturn(false);
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        service.initializeSession(SESSION_ID, owner);

        verify(hashOperations).put(SESSION_KEY, "userId", owner.toString());
        verify(redisTemplate).expire(SESSION_KEY, Duration.ofHours(1));
    }

    // 방어선이 두 곳이라 각각 테스트한다 -> initializeSession(신규 연결 경로)과
    // validateSessionOwner(메시지 전송·저장·오프너 분석 등 6개 지점)가 같은 규칙을 지켜야 한다
    @Test
    @DisplayName("남의 세션에 접근하면 소유자 검증이 SESSION_ACCESS_DENIED 로 거부한다")
    void validateSessionOwner_ownerMismatch_throw() {
        UUID owner = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();

        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(owner.toString());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateSessionOwner(SESSION_ID, attacker));

        assertEquals(ErrorCode.SESSION_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("세션이 존재하지 않으면 소유자를 확인할 수 없으므로 거부한다")
    void validateSessionOwner_sessionAbsent_throw() {
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(null);
        doReturn(masterHashOperations).when(masterRedisTemplate).opsForHash();
        when(masterHashOperations.get(SESSION_KEY, "userId")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateSessionOwner(SESSION_ID, UUID.randomUUID()));

        assertEquals(ErrorCode.SESSION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("소유자 본인이면 검증을 통과한다")
    void validateSessionOwner_sameOwner_pass() {
        UUID owner = UUID.randomUUID();

        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(owner.toString());

        assertDoesNotThrow(() -> service.validateSessionOwner(SESSION_ID, owner));
    }

    @Test
    @DisplayName("Redis I/O 실패는 도메인 예외로 감싸지 않고 그대로 전파한다")
    void validateSessionOwner_레디스장애_DataAccessException을전파한다() {
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId"))
                .thenThrow(new RedisConnectionFailureException("node down"));

        assertThrows(RedisConnectionFailureException.class,
                () -> service.validateSessionOwner(SESSION_ID, UUID.randomUUID()));
    }

    @Test
    @DisplayName("메시지 저장이 실패해도 예외 대신 드롭 메트릭만 올린다")
    void saveMessageFallback_레디스장애_예외없이드롭된다() {
        assertDoesNotThrow(() -> service.saveMessageFallbackForTest(
                SESSION_ID, null, new RedisConnectionFailureException("io")));

        assertEquals(1.0,
                meterRegistry.counter("chat.redis.save.dropped", "reason", "io_error").count());
    }

    @Test
    @DisplayName("저장 fallback 도 도메인 예외는 그대로 던진다")
    void saveMessageFallback_도메인예외_그대로던진다() {
        BusinessException domain = new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);

        BusinessException thrown = assertThrows(BusinessException.class,
                () -> service.saveMessageFallbackForTest(SESSION_ID, null, domain));

        assertEquals(ErrorCode.MESSAGE_INPUT_FAIL, thrown.getErrorCode());
    }

    @Test
    @DisplayName("프롬프트용 최근 히스토리는 master 에서 읽는다")
    void getRecentSessionMessages_항상_master로읽는다() {
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        when(masterListOperations.range(MESSAGE_KEY, -20L, -1L)).thenReturn(List.of());

        service.getRecentSessionMessages(SESSION_ID);

        // 같은 요청 안에서 saveMessage 직후에 읽으므로 replica 를 타면 방금 쓴 질문이 빠진다
        verify(masterListOperations).range(MESSAGE_KEY, -20L, -1L);
        verify(redisTemplate, never()).opsForList();
    }

    @Test
    @DisplayName("영속화용 전량 조회는 replica 로 읽어 분산을 유지한다")
    void getSessionMessages_replica로읽는다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, -1L)).thenReturn(List.of());

        service.getSessionMessages(SESSION_ID);

        verify(listOperations).range(MESSAGE_KEY, 0L, -1L);
        verify(masterRedisTemplate, never()).opsForList();
    }

    @Test
    @DisplayName("소유자 검증은 replica 로 읽고 결과가 있으면 master 를 치지 않는다")
    void validateSessionOwner_replica에서찾음_master조회없다() {
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(USER_ID.toString());

        service.validateSessionOwner(SESSION_ID, USER_ID);

        verify(masterRedisTemplate, never()).opsForHash();
    }

    @Test
    @DisplayName("소유자 검증이 replica 에서 비면 master 로 한 번 더 확인한다")
    void validateSessionOwner_replica가비었음_master로재확인한다() {
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(null);
        doReturn(masterHashOperations).when(masterRedisTemplate).opsForHash();
        when(masterHashOperations.get(SESSION_KEY, "userId")).thenReturn(USER_ID.toString());

        // 복제 지연으로 replica 가 아직 못 본 세션을 없는 것으로 단정하면 방금 만든 세션이 거부된다
        assertDoesNotThrow(() -> service.validateSessionOwner(SESSION_ID, USER_ID));

        verify(masterHashOperations).get(SESSION_KEY, "userId");
    }
}
