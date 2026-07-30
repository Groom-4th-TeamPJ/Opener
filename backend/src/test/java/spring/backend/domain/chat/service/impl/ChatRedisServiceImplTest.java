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
import org.springframework.data.redis.core.HashOperations;
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

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;

    private ChatRedisServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatRedisServiceImpl(new ObjectMapper(), redisTemplate, 20);
    }

    @Test
    @DisplayName("기존 세션 키의 소유자가 다르면 INVALID_SESSION 으로 재사용을 거부한다")
    void initializeSession_ownerMismatch_throw() {
        UUID owner = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();

        when(redisTemplate.hasKey(SESSION_KEY)).thenReturn(true);
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(owner.toString());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.initializeSession(SESSION_ID, attacker));

        assertEquals(ErrorCode.INVALID_SESSION, ex.getErrorCode());

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

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.initializeSession(SESSION_ID, UUID.randomUUID()));

        assertEquals(ErrorCode.INVALID_SESSION, ex.getErrorCode());
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
    @DisplayName("남의 세션에 접근하면 소유자 검증이 INVALID_SESSION 으로 거부한다")
    void validateSessionOwner_ownerMismatch_throw() {
        UUID owner = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();

        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(owner.toString());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateSessionOwner(SESSION_ID, attacker));

        assertEquals(ErrorCode.INVALID_SESSION, ex.getErrorCode());
    }

    @Test
    @DisplayName("세션이 존재하지 않으면 소유자를 확인할 수 없으므로 거부한다")
    void validateSessionOwner_sessionAbsent_throw() {
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.validateSessionOwner(SESSION_ID, UUID.randomUUID()));

        assertEquals(ErrorCode.INVALID_SESSION, ex.getErrorCode());
    }

    @Test
    @DisplayName("소유자 본인이면 검증을 통과한다")
    void validateSessionOwner_sameOwner_pass() {
        UUID owner = UUID.randomUUID();

        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(owner.toString());

        assertDoesNotThrow(() -> service.validateSessionOwner(SESSION_ID, owner));
    }
}
