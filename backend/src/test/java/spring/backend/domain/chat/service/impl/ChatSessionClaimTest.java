package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

// 세션 선점이 복제본 hasKey + 분리된 HSET/EXPIRE 였을 때의 두 결함을 막는 회귀 테스트
// (1) 동시 connect 2건이 모두 "세션 없음" 을 보고 둘 다 기록 -> 나중 쪽이 소유자가 된다
// (2) HSET 과 EXPIRE 사이에서 끊기면 만료 없는 키가 남는다
@ExtendWith(MockitoExtension.class)
class ChatSessionClaimTest {

    private static final Long SESSION_ID = 42L;
    private static final String SESSION_KEY = "chat:{42}:session";
    private static final UUID USER_ID = UUID.randomUUID();
    // 세션 TTL 1시간 -> 스크립트 인자는 문자열 초 단위
    private static final String TTL_SECONDS = "3600";

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private StringRedisTemplate masterRedisTemplate;

    private ChatRedisServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatRedisServiceImpl(
                new ObjectMapper(), redisTemplate, 20, new SimpleMeterRegistry(), masterRedisTemplate);
    }

    @Test
    @DisplayName("신규 세션 선점은 마스터 템플릿의 스크립트 실행 한 번으로 끝난다")
    void initializeSession_신규세션_마스터에서스크립트한번으로선점한다() {
        claimReturns(1L);

        service.initializeSession(SESSION_ID, USER_ID);

        // HSET 과 EXPIRE 가 한 왕복이어야 둘 사이에서 끊겨도 만료 없는 키가 남지 않는다
        verify(masterRedisTemplate, times(1))
                .execute(any(RedisScript.class), eq(List.of(SESSION_KEY)),
                        eq(USER_ID.toString()), eq(TTL_SECONDS), any());
        verifyNoMoreInteractions(masterRedisTemplate);
    }

    @Test
    @DisplayName("선점은 복제본을 읽지 않는다")
    void initializeSession_복제본을읽지않는다() {
        claimReturns(1L);

        service.initializeSession(SESSION_ID, USER_ID);

        // 복제본 hasKey 는 서킷 밖이고 복제 지연 창에서 방금 만든 세션을 "없음" 으로 읽는다
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("선점 스크립트가 HSETNX 와 EXPIRE 를 함께 담는다")
    @SuppressWarnings("unchecked")
    void initializeSession_선점스크립트에HSETNX와EXPIRE가함께있다() throws Exception {
        claimReturns(1L);

        service.initializeSession(SESSION_ID, USER_ID);

        ArgumentCaptor<RedisScript<Long>> script = ArgumentCaptor.forClass(RedisScript.class);
        verify(masterRedisTemplate).execute(script.capture(), eq(List.of(SESSION_KEY)),
                eq(USER_ID.toString()), eq(TTL_SECONDS), any());
        String lua = script.getValue().getScriptAsString();
        // HSETNX 가 "없을 때만 쓴다" 를 한 명령으로 판정한다 -> 뒤의 사용자는 소유자가 되지 못한다
        assertTrue(lua.contains("HSETNX"), "선점 판정이 HSETNX 가 아님: " + lua);
        assertTrue(lua.contains("EXPIRE"), "TTL 이 스크립트 밖에 있음: " + lua);
    }

    @Test
    @DisplayName("다른 사용자가 이미 소유한 세션이면 SESSION_ACCESS_DENIED 로 거부한다")
    void initializeSession_결과0_SESSION_ACCESS_DENIED로거부한다() {
        claimReturns(0L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.initializeSession(SESSION_ID, USER_ID));

        assertEquals(ErrorCode.SESSION_ACCESS_DENIED, ex.getErrorCode());
    }

    @Test
    @DisplayName("선점에 성공하거나 이미 내 세션이면 예외 없이 통과한다")
    void initializeSession_결과1_예외없이통과한다() {
        claimReturns(1L);

        assertDoesNotThrow(() -> service.initializeSession(SESSION_ID, USER_ID));
    }

    @Test
    @DisplayName("선점은 redis-chat 서킷 안에서 실행된다")
    void initializeSession_redisChat서킷이걸려있다() throws Exception {
        CircuitBreaker breaker = ChatRedisServiceImpl.class
                .getMethod("initializeSession", Long.class, UUID.class)
                .getAnnotation(CircuitBreaker.class);

        // 서킷 밖에서 실행되면 Redis 장애 때 스레드가 타임아웃 대기에 묶인다
        assertEquals("redis-chat", breaker == null ? null : breaker.name());
    }

    @SuppressWarnings("unchecked")
    private void claimReturns(Long result) {
        when(masterRedisTemplate.execute(any(RedisScript.class), eq(List.of(SESSION_KEY)), any(), any(), any()))
                .thenReturn(result);
    }
}
