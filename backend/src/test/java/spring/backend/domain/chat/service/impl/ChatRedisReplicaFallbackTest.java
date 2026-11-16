package spring.backend.domain.chat.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

// replica 읽기가 Redis 오류로 실패하면 요청을 실패시키지 않고 같은 읽기를 master 에서 한 번 한다
// master 오류는 그대로 전파한다 -> redis-chat 서킷이 그 실패를 센다
@ExtendWith(MockitoExtension.class)
class ChatRedisReplicaFallbackTest {

    private static final Long SESSION_ID = 42L;
    private static final String SESSION_KEY = "chat:{42}:session";
    private static final String MESSAGE_KEY = "chat:{42}:messages";

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private StringRedisTemplate masterRedisTemplate;
    @Mock private ListOperations<String, String> listOperations;
    @Mock private ListOperations<String, String> masterListOperations;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private HashOperations<String, Object, Object> masterHashOperations;

    private ChatRedisServiceImpl service;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new ChatRedisServiceImpl(new ObjectMapper(), redisTemplate, 20, meterRegistry, masterRedisTemplate);
    }

    private static String json(int n) {
        return "{\"role\":\"" + (n % 2 == 1 ? "USER" : "LLM") + "\",\"message\":\"m" + n
                + "\",\"timestamp\":null,\"partial\":false}";
    }

    private static List<String> jsons(int fromInclusive, int toInclusive) {
        return java.util.stream.IntStream.rangeClosed(fromInclusive, toInclusive)
                .mapToObj(ChatRedisReplicaFallbackTest::json).toList();
    }

    private static List<String> texts(List<RedisMessageDto> messages) {
        return messages.stream().map(RedisMessageDto::message).toList();
    }

    private double replicaErrors(String op) {
        return meterRegistry.counter("chat.redis.read.replica_error", "op", op).count();
    }

    // ---- getHistoryBefore ----

    @Test
    @DisplayName("history: replica 가 Redis 오류를 내면 같은 범위를 master 에서 한 번 읽어 돌려주고 history 오류를 센다")
    void getHistoryBefore_replica오류_master로같은범위를한번읽는다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, 5L)).thenThrow(new RedisConnectionFailureException("replica down"));
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        when(masterListOperations.range(MESSAGE_KEY, 0L, 5L)).thenReturn(jsons(1, 6));

        List<RedisMessageDto> history = service.getHistoryBefore(SESSION_ID, 7L);

        assertEquals(List.of("m1", "m2", "m3", "m4", "m5", "m6"), texts(history));
        verify(masterListOperations, times(1)).range(MESSAGE_KEY, 0L, 5L);
        assertEquals(1.0, replicaErrors("history"));
        // 오류 경로의 master 읽기는 짧은 결과 fallback 이 아니다 -> 빈 결과를 내는 결함이 있어도 그 fallback 이 같은 master 를 읽어 결과만으로는 안 드러난다
        assertEquals(0.0, meterRegistry.counter("chat.redis.history.fallback").count());
    }

    @Test
    @DisplayName("history: Lettuce RedisException 도 같은 방식으로 master 읽기로 넘어간다")
    void getHistoryBefore_lettuce예외_master로읽는다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, 5L)).thenThrow(new RedisException("channel closed"));
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        when(masterListOperations.range(MESSAGE_KEY, 0L, 5L)).thenReturn(jsons(1, 6));

        assertEquals(6, service.getHistoryBefore(SESSION_ID, 7L).size());
        assertEquals(1.0, replicaErrors("history"));
    }

    @Test
    @DisplayName("history: replica 가 제대로 답하면 master 를 건드리지 않고 오류를 세지 않는다")
    void getHistoryBefore_replica정상_master를치지않는다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, 5L)).thenReturn(jsons(1, 6));

        assertEquals(6, service.getHistoryBefore(SESSION_ID, 7L).size());

        verify(masterRedisTemplate, never()).opsForList();
        assertEquals(0.0, replicaErrors("history"));
    }

    @Test
    @DisplayName("history: master 읽기도 실패하면 그 오류가 올라간다")
    void getHistoryBefore_master도오류_전파한다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, 5L)).thenThrow(new RedisConnectionFailureException("replica down"));
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        when(masterListOperations.range(MESSAGE_KEY, 0L, 5L)).thenThrow(new RedisConnectionFailureException("master down"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getHistoryBefore(SESSION_ID, 7L));

        assertEquals(ErrorCode.MESSAGE_INPUT_FAIL, ex.getErrorCode());
        assertEquals(1.0, replicaErrors("history"));
    }

    @Test
    @DisplayName("history: replica 결과가 범위보다 짧은 기존 fallback 은 오류 계수 없이 그대로 동작한다")
    void getHistoryBefore_replica짧음_기존fallback은오류를세지않는다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, 5L)).thenReturn(jsons(1, 4));
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        when(masterListOperations.range(MESSAGE_KEY, 0L, 5L)).thenReturn(jsons(1, 6));

        assertEquals(6, service.getHistoryBefore(SESSION_ID, 7L).size());

        assertEquals(1.0, meterRegistry.counter("chat.redis.history.fallback").count());
        assertEquals(0.0, replicaErrors("history"));
    }

    // ---- getSessionMessages ----

    @Test
    @DisplayName("session_messages: replica 오류면 전체 범위를 master 에서 한 번 읽는다")
    void getSessionMessages_replica오류_master로전체범위를읽는다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, -1L)).thenThrow(new RedisConnectionFailureException("replica down"));
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        when(masterListOperations.range(MESSAGE_KEY, 0L, -1L)).thenReturn(jsons(1, 4));

        List<RedisMessageDto> messages = service.getSessionMessages(SESSION_ID);

        assertEquals(List.of("m1", "m2", "m3", "m4"), texts(messages));
        verify(masterListOperations, times(1)).range(MESSAGE_KEY, 0L, -1L);
        assertEquals(1.0, replicaErrors("session_messages"));
    }

    @Test
    @DisplayName("session_messages: replica 가 답하면 master 를 건드리지 않는다")
    void getSessionMessages_replica정상_master를치지않는다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, -1L)).thenReturn(jsons(1, 4));

        assertEquals(4, service.getSessionMessages(SESSION_ID).size());

        verify(masterRedisTemplate, never()).opsForList();
        assertEquals(0.0, replicaErrors("session_messages"));
    }

    @Test
    @DisplayName("session_messages: master 읽기도 실패하면 그 오류가 올라간다")
    void getSessionMessages_master도오류_전파한다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, -1L)).thenThrow(new RedisException("replica closed"));
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        when(masterListOperations.range(MESSAGE_KEY, 0L, -1L)).thenThrow(new RedisConnectionFailureException("master down"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getSessionMessages(SESSION_ID));

        assertEquals(ErrorCode.MESSAGE_INPUT_FAIL, ex.getErrorCode());
    }

    // ---- 소유자 확인 ----

    @Test
    @DisplayName("owner: replica 오류면 같은 키를 master 에서 한 번 읽고 그 값으로 검증을 마친다")
    void validateSessionOwner_replica오류_master값으로통과한다() {
        UUID owner = UUID.randomUUID();
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenThrow(new RedisConnectionFailureException("replica down"));
        doReturn(masterHashOperations).when(masterRedisTemplate).opsForHash();
        when(masterHashOperations.get(SESSION_KEY, "userId")).thenReturn(owner.toString());

        assertDoesNotThrow(() -> service.validateSessionOwner(SESSION_ID, owner));

        verify(masterHashOperations, times(1)).get(SESSION_KEY, "userId");
        assertEquals(1.0, replicaErrors("owner"));
    }

    @Test
    @DisplayName("owner: replica 가 소유자를 돌려주면 master 를 건드리지 않는다")
    void validateSessionOwner_replica정상_master를치지않는다() {
        UUID owner = UUID.randomUUID();
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(owner.toString());

        assertDoesNotThrow(() -> service.validateSessionOwner(SESSION_ID, owner));

        verify(masterRedisTemplate, never()).opsForHash();
        assertEquals(0.0, replicaErrors("owner"));
    }

    @Test
    @DisplayName("owner: replica 가 비어 있을 때의 master 재확인은 오류 계수 없이 그대로 동작한다")
    void validateSessionOwner_replica비어있음_기존master재확인은오류를세지않는다() {
        UUID owner = UUID.randomUUID();
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenReturn(null);
        doReturn(masterHashOperations).when(masterRedisTemplate).opsForHash();
        when(masterHashOperations.get(SESSION_KEY, "userId")).thenReturn(owner.toString());

        assertDoesNotThrow(() -> service.validateSessionOwner(SESSION_ID, owner));

        assertEquals(0.0, replicaErrors("owner"));
    }

    @Test
    @DisplayName("owner: master 읽기도 실패하면 Redis 오류가 감싸지 않고 올라간다")
    void validateSessionOwner_master도오류_그대로전파한다() {
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        when(hashOperations.get(SESSION_KEY, "userId")).thenThrow(new RedisConnectionFailureException("replica down"));
        doReturn(masterHashOperations).when(masterRedisTemplate).opsForHash();
        when(masterHashOperations.get(SESSION_KEY, "userId")).thenThrow(new RedisConnectionFailureException("master down"));

        assertThrows(RedisConnectionFailureException.class,
                () -> service.validateSessionOwner(SESSION_ID, UUID.randomUUID()));
    }
}
