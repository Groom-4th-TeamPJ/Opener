package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.script.RedisScript;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.data.redis.RedisConnectionFailureException;
import java.util.List;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    // 방어선이 두 곳이라 각각 테스트한다 -> initializeSession(신규 연결 경로, ChatSessionClaimTest)과
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
        long length = assertDoesNotThrow(() -> service.saveMessageFallbackForTest(
                SESSION_ID, null, new RedisConnectionFailureException("io")));

        // 길이를 모른다는 표시 -> 호출자가 0 으로 받으면 질문 직전 히스토리를 읽지 못한다
        assertEquals(-1L, length);
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

    // 저장 한 번 = 스크립트 한 번 -> RPUSH · EXPIRE 2회를 따로 보내던 왕복 3번이 1번이 된다
    @Test
    @DisplayName("메시지 저장은 스크립트 한 번으로 push 와 TTL 갱신을 하고 목록 길이를 돌려준다")
    void saveMessage_스크립트한번_길이를돌려준다() throws Exception {
        RedisMessageDto message = requestMessage();
        String json = new ObjectMapper().writeValueAsString(message);
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(MESSAGE_KEY, SESSION_KEY)), eq(json), eq("3600"))).thenReturn(3L);

        long length = service.saveMessage(SESSION_ID, message);

        assertEquals(3L, length);
        verify(redisTemplate, times(1)).execute(ArgumentMatchers.<RedisScript<Long>>any(),
                eq(List.of(MESSAGE_KEY, SESSION_KEY)), eq(json), eq("3600"));
        // 스크립트 밖에서 따로 보내는 명령이 남아 있으면 왕복이 다시 늘어난다
        verify(redisTemplate, never()).opsForList();
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("스크립트가 결과를 돌려주지 않으면 길이를 알 수 없는 값(-1)을 돌려준다")
    void saveMessage_스크립트결과없음_마이너스1() {
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), anyString(), anyString()))
                .thenReturn(null);

        assertEquals(-1L, service.saveMessage(SESSION_ID, requestMessage()));
    }

    @Test
    @DisplayName("저장 fallback 은 두 경로 모두 -1 을 돌려주고 드롭 메트릭을 올린다")
    void saveMessageFallback_서킷열림_마이너스1과드롭메트릭() {
        CircuitBreaker open = CircuitBreaker.ofDefaults("chat-save-test");

        long length = service.saveMessageFallbackForTest(
                SESSION_ID, requestMessage(), CallNotPermittedException.createCallNotPermittedException(open));

        assertEquals(-1L, length);
        assertEquals(1.0, meterRegistry.counter("chat.redis.save.dropped", "reason", "circuit_open").count());
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

    private static final String USER_JSON = "{\"role\":\"USER\",\"message\":\"요청\",\"timestamp\":null,\"partial\":false}";
    private static final String LLM_JSON = "{\"role\":\"LLM\",\"message\":\"앞선 답\",\"timestamp\":null,\"partial\":false}";
    private static final String PARTIAL_JSON = "{\"role\":\"LLM\",\"message\":\"끊긴 답\",\"timestamp\":null,\"partial\":true}";

    private RedisMessageDto requestMessage() {
        return RedisMessageDto.builder().chatRole(ChatRole.USER).message("요청").build();
    }

    @Test
    @DisplayName("마지막 항목이 방금 저장하려던 USER 메시지면 그 하나만 꺼낸다")
    void removeLastMessage_마지막이같은USER메시지_꺼낸다() {
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        when(masterListOperations.index(MESSAGE_KEY, -1L)).thenReturn(USER_JSON);

        service.removeLastMessage(SESSION_ID, requestMessage());

        // 세션 해시나 목록 전체를 지우면 같은 세션의 앞선 대화까지 사라진다
        verify(masterListOperations).rightPop(MESSAGE_KEY);
        verify(masterRedisTemplate, never()).delete(anyString());
        // 방금 쓴 항목을 읽는 경로라 복제 지연이 있으면 replica 가 앞 턴의 꼬리를 돌려준다 -> replica 는 건드리지 않는다
        verify(redisTemplate, never()).opsForList();
    }

    @Test
    @DisplayName("마지막 항목이 LLM 답이면 아무것도 꺼내지 않는다")
    void removeLastMessage_마지막이LLM답_꺼내지않는다() {
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        // 요청 메시지 저장이 saveMessageFallback 에서 버려졌다면 마지막은 앞 턴의 답이다
        when(masterListOperations.index(MESSAGE_KEY, -1L)).thenReturn(LLM_JSON);

        service.removeLastMessage(SESSION_ID, requestMessage());

        verify(masterListOperations, never()).rightPop(anyString());
    }

    @Test
    @DisplayName("마지막 항목의 USER 내용이 다르거나 목록이 비면 아무것도 꺼내지 않는다")
    void removeLastMessage_내용이다르거나빈목록_꺼내지않는다() {
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        when(masterListOperations.index(MESSAGE_KEY, -1L)).thenReturn(
                "{\"role\":\"USER\",\"message\":\"다른 질문\",\"timestamp\":null,\"partial\":false}", (String) null);

        service.removeLastMessage(SESSION_ID, requestMessage());
        service.removeLastMessage(SESSION_ID, requestMessage());

        verify(masterListOperations, never()).rightPop(anyString());
    }

    @Test
    @DisplayName("프롬프트용 히스토리는 partial 표식 메시지를 제외하되 길이 판정에는 그 항목도 센다")
    void getHistoryBefore_partial표식_프롬프트에서제외한다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        // L=3 이면 [0, 1] 두 항목이 와야 정상이다 -> partial 도 한 항목으로 세야 복제 지연으로 오인하지 않는다
        when(listOperations.range(MESSAGE_KEY, 0L, 1L)).thenReturn(List.of(USER_JSON, PARTIAL_JSON));

        List<RedisMessageDto> history = service.getHistoryBefore(SESSION_ID, 3L);

        // 끊긴 답이 다음 턴 프롬프트에 들어가면 모델이 끝나지 않은 문장을 자기 답으로 이어 쓴다
        assertEquals(List.of("요청"), history.stream().map(RedisMessageDto::message).toList());
        assertEquals(0.0, fallbackCount());
        verify(masterRedisTemplate, never()).opsForList();
    }

    private static String json(int n) {
        return "{\"role\":\"" + (n % 2 == 1 ? "USER" : "LLM") + "\",\"message\":\"m" + n
                + "\",\"timestamp\":null,\"partial\":false}";
    }

    private static List<String> jsons(int fromInclusive, int toInclusive) {
        return java.util.stream.IntStream.rangeClosed(fromInclusive, toInclusive).mapToObj(ChatRedisServiceImplTest::json).toList();
    }

    private static List<String> texts(List<RedisMessageDto> messages) {
        return messages.stream().map(RedisMessageDto::message).toList();
    }

    private double fallbackCount() {
        return meterRegistry.counter("chat.redis.history.fallback").count();
    }

    private long readTimerCount(String route) {
        return meterRegistry.timer("chat.redis.read", "route", route).count();
    }

    @Test
    @DisplayName("L=7 이면 replica 에서 [0, 5] 를 읽고 항목이 다 오면 master 를 치지 않는다")
    void getHistoryBefore_replica가충분함_replica만읽는다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, 5L)).thenReturn(jsons(1, 6));

        List<RedisMessageDto> history = service.getHistoryBefore(SESSION_ID, 7L);

        assertEquals(List.of("m1", "m2", "m3", "m4", "m5", "m6"), texts(history));
        verify(masterRedisTemplate, never()).opsForList();
        assertEquals(0.0, fallbackCount());
        assertEquals(1L, readTimerCount("replica"));
    }

    @Test
    @DisplayName("L 이 윈도우보다 크면 시작 인덱스를 max(0, L-W) 로 올려 최근 W-1 개만 읽는다")
    void getHistoryBefore_윈도우보다긴목록_뒤쪽범위만읽는다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        // L=30, W=20 -> from=10, 끝=28 -> 19 개
        when(listOperations.range(MESSAGE_KEY, 10L, 28L)).thenReturn(jsons(11, 29));

        List<RedisMessageDto> history = service.getHistoryBefore(SESSION_ID, 30L);

        assertEquals(19, history.size());
        assertEquals("m11", history.get(0).message());
        assertEquals("m29", history.get(18).message());
        verify(listOperations).range(MESSAGE_KEY, 10L, 28L);
    }

    @Test
    @DisplayName("윈도우가 0 이하이면 L 에 상관없이 처음부터 L-2 까지 읽는다")
    void getHistoryBefore_윈도우비활성_처음부터읽는다() {
        service = new ChatRedisServiceImpl(new ObjectMapper(), redisTemplate, 0, meterRegistry, masterRedisTemplate);
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, 28L)).thenReturn(jsons(1, 29));

        assertEquals(29, service.getHistoryBefore(SESSION_ID, 30L).size());
    }

    @Test
    @DisplayName("replica 결과가 범위보다 짧으면 master 에서 같은 범위를 한 번 다시 읽고 fallback 을 센다")
    void getHistoryBefore_replica가짧음_master로한번재조회한다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, 5L)).thenReturn(jsons(1, 3));
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        when(masterListOperations.range(MESSAGE_KEY, 0L, 5L)).thenReturn(jsons(1, 6));

        List<RedisMessageDto> history = service.getHistoryBefore(SESSION_ID, 7L);

        // 복제가 방금 쓴 항목까지 못 따라왔다 -> 짧은 결과를 그대로 쓰면 직전 답이 프롬프트에서 빠진다
        assertEquals(6, history.size());
        verify(listOperations, times(1)).range(MESSAGE_KEY, 0L, 5L);
        verify(masterListOperations, times(1)).range(MESSAGE_KEY, 0L, 5L);
        assertEquals(1.0, fallbackCount());
        assertEquals(1L, readTimerCount("replica"));
        assertEquals(1L, readTimerCount("master-fallback"));
    }

    @Test
    @DisplayName("replica 가 아예 비어 있어도 master 로 한 번 재조회한다")
    void getHistoryBefore_replica가비었음_master로재조회한다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, 0L)).thenReturn(List.of());
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        when(masterListOperations.range(MESSAGE_KEY, 0L, 0L)).thenReturn(jsons(1, 1));

        assertEquals(List.of("m1"), texts(service.getHistoryBefore(SESSION_ID, 2L)));
        assertEquals(1.0, fallbackCount());
    }

    @Test
    @DisplayName("L 이 0 또는 1 이면 읽을 이전 메시지가 없으므로 어느 쪽도 읽지 않는다")
    void getHistoryBefore_이전메시지없음_읽지않는다() {
        assertEquals(List.of(), service.getHistoryBefore(SESSION_ID, 0L));
        assertEquals(List.of(), service.getHistoryBefore(SESSION_ID, 1L));

        verify(redisTemplate, never()).opsForList();
        verify(masterRedisTemplate, never()).opsForList();
        assertEquals(0.0, fallbackCount());
    }

    @Test
    @DisplayName("저장이 버려진 턴(L<0)은 master 에서 마지막 W-1 개를 읽는다")
    void getHistoryBefore_저장버려짐_master에서최근W빼기1개를읽는다() {
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        // 질문은 저장되지 않았으므로 마지막 W-1 개가 곧 이전 대화이고, 질문 자리는 프롬프트 조립이 채운다
        when(masterListOperations.range(MESSAGE_KEY, -19L, -1L)).thenReturn(jsons(1, 4));

        List<RedisMessageDto> history = service.getHistoryBefore(SESSION_ID, -1L);

        assertEquals(4, history.size());
        verify(redisTemplate, never()).opsForList();
        assertEquals(0.0, fallbackCount());
        assertEquals(1L, readTimerCount("master"));
    }

    @Test
    @DisplayName("저장이 버려진 턴에 윈도우가 0 이하이면 master 전체를 읽는다")
    void getHistoryBefore_저장버려짐_윈도우비활성_전체를읽는다() {
        service = new ChatRedisServiceImpl(new ObjectMapper(), redisTemplate, 0, meterRegistry, masterRedisTemplate);
        doReturn(masterListOperations).when(masterRedisTemplate).opsForList();
        when(masterListOperations.range(MESSAGE_KEY, 0L, -1L)).thenReturn(jsons(1, 4));

        assertEquals(4, service.getHistoryBefore(SESSION_ID, -1L).size());
    }

    @Test
    @DisplayName("윈도우가 1 이면 이전 메시지를 싣지 않으므로 어느 쪽도 읽지 않는다")
    void getHistoryBefore_윈도우1_읽지않는다() {
        service = new ChatRedisServiceImpl(new ObjectMapper(), redisTemplate, 1, meterRegistry, masterRedisTemplate);

        // -(W-1) = 0 이 "처음부터" 로 읽혀 전체가 실리는 것을 막는다
        assertEquals(List.of(), service.getHistoryBefore(SESSION_ID, -1L));
        assertEquals(List.of(), service.getHistoryBefore(SESSION_ID, 9L));

        verify(redisTemplate, never()).opsForList();
        verify(masterRedisTemplate, never()).opsForList();
    }

    @Test
    @DisplayName("영속화용 전량 조회는 partial 표식 메시지를 그대로 돌려준다")
    void getSessionMessages_partial표식_영속화읽기에는남는다() {
        doReturn(listOperations).when(redisTemplate).opsForList();
        when(listOperations.range(MESSAGE_KEY, 0L, -1L)).thenReturn(List.of(USER_JSON, PARTIAL_JSON));

        List<RedisMessageDto> all = service.getSessionMessages(SESSION_ID);

        // 소비자가 표식을 보고 직접 제외한다 -> 여기서 빼면 제외한 사실을 메트릭으로 남길 수 없다
        assertEquals(2, all.size());
        assertEquals(true, all.get(1).partial());
    }
}
