package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 히스토리 읽기의 범위 계산과 복제 지연 감지를 실제 Redis 로 확인한다
// 같은 서버의 0 번 DB 를 마스터, 1 번 DB 를 복제본 대역으로 쓴다 -> 한쪽에만 쓰면 복제가 덜 따라온 상태가 된다
// REDIS_TEST_PORT 가 있을 때만 돈다: docker run -d --rm -p 16399:6379 --name r125-t1-redis redis:7
// 실행: REDIS_TEST_PORT=16399 ./gradlew cleanTest test --tests '*ChatHistoryReadRedisTest'
@EnabledIfEnvironmentVariable(named = "REDIS_TEST_PORT", matches = ".+")
class ChatHistoryReadRedisTest {

    private static LettuceConnectionFactory masterFactory;
    private static LettuceConnectionFactory replicaFactory;
    private static StringRedisTemplate master;
    private static StringRedisTemplate replica;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SimpleMeterRegistry meterRegistry;
    private Long sessionId;
    private String messageKey;

    @BeforeAll
    static void connect() {
        int port = Integer.parseInt(System.getenv("REDIS_TEST_PORT"));
        masterFactory = factory(port, 0);
        replicaFactory = factory(port, 1);
        master = new StringRedisTemplate(masterFactory);
        replica = new StringRedisTemplate(replicaFactory);
    }

    private static LettuceConnectionFactory factory(int port, int database) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", port);
        config.setDatabase(database);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        return factory;
    }

    @AfterAll
    static void disconnect() {
        if (masterFactory != null) {
            masterFactory.destroy();
        }
        if (replicaFactory != null) {
            replicaFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        sessionId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        messageKey = "chat:{" + sessionId + "}:messages";
    }

    @AfterEach
    void cleanUp() {
        master.delete(messageKey);
        replica.delete(messageKey);
    }

    private ChatRedisServiceImpl service(int window) {
        return new ChatRedisServiceImpl(objectMapper, replica, window, meterRegistry, master);
    }

    private String json(int n) {
        return "{\"role\":\"" + (n % 2 == 1 ? "USER" : "LLM") + "\",\"message\":\"m" + n
                + "\",\"timestamp\":null,\"partial\":false}";
    }

    private void seed(StringRedisTemplate template, int count) {
        template.opsForList().rightPushAll(messageKey, IntStream.rangeClosed(1, count).mapToObj(this::json).toList());
    }

    private List<String> texts(List<RedisMessageDto> messages) {
        return messages.stream().map(RedisMessageDto::message).toList();
    }

    private double fallbacks() {
        return meterRegistry.counter("chat.redis.history.fallback").count();
    }

    @Test
    @DisplayName("마스터와 복제본이 같은 7 개를 가지면 L=7 은 1~6 번을 순서대로 돌려주고 fallback 은 0 이다")
    void getHistoryBefore_복제본이최신_1부터6을읽고fallback없다() {
        seed(master, 7);
        seed(replica, 7);

        List<RedisMessageDto> history = service(20).getHistoryBefore(sessionId, 7L);

        assertEquals(List.of("m1", "m2", "m3", "m4", "m5", "m6"), texts(history));
        assertEquals(0.0, fallbacks());
    }

    @Test
    @DisplayName("복제본이 5 개만 가지면(복제 지연) 마스터에서 1~6 번을 읽고 fallback 은 1 이다")
    void getHistoryBefore_복제본이뒤처짐_마스터에서읽고fallback이1() {
        seed(master, 7);
        seed(replica, 5);

        List<RedisMessageDto> history = service(20).getHistoryBefore(sessionId, 7L);

        // 복제본 결과(1~5 번)를 그대로 쓰면 직전 LLM 답(6 번)이 프롬프트에서 빠진다
        assertEquals(List.of("m1", "m2", "m3", "m4", "m5", "m6"), texts(history));
        assertEquals(1.0, fallbacks());
    }

    @Test
    @DisplayName("윈도우 4, L=7 이면 4~6 번만 돌려준다")
    void getHistoryBefore_윈도우4_4부터6만읽는다() {
        seed(master, 7);
        seed(replica, 7);

        List<RedisMessageDto> history = service(4).getHistoryBefore(sessionId, 7L);

        assertEquals(List.of("m4", "m5", "m6"), texts(history));
        assertEquals(0.0, fallbacks());
    }

    @Test
    @DisplayName("저장 한 번의 반환 길이를 그대로 넘기면 방금 저장한 질문은 빠지고 이전 대화만 읽힌다")
    void saveMessage_반환길이로읽기_방금질문은빠진다() {
        // 저장과 읽기를 같은 DB 로 -> 스크립트가 쓴 키를 읽기 범위가 그대로 가리키는지 본다
        ChatRedisServiceImpl sameDb = new ChatRedisServiceImpl(objectMapper, master, 20, meterRegistry, master);
        RedisMessageDto question1 = RedisMessageDto.builder().chatRole(ChatRole.USER).message("m1").build();
        RedisMessageDto answer1 = RedisMessageDto.builder().chatRole(ChatRole.LLM).message("m2").build();
        RedisMessageDto question2 = RedisMessageDto.builder().chatRole(ChatRole.USER).message("m3").build();

        assertEquals(1L, sameDb.saveMessage(sessionId, question1));
        assertEquals(2L, sameDb.saveMessage(sessionId, answer1));
        long written = sameDb.saveMessage(sessionId, question2);

        assertEquals(3L, written);
        assertEquals(List.of("m1", "m2"), texts(sameDb.getHistoryBefore(sessionId, written)));
        Long ttl = master.getExpire(messageKey);
        assertTrue(ttl != null && ttl >= 3590 && ttl <= 3600, "메시지 키 TTL: " + ttl);
    }

    @Test
    @DisplayName("저장이 버려진 턴(L=-1)은 마스터의 마지막 W-1 개를 읽는다")
    void getHistoryBefore_저장버려짐_마스터의최근W빼기1개() {
        seed(master, 8);

        List<RedisMessageDto> history = service(4).getHistoryBefore(sessionId, -1L);

        assertEquals(List.of("m6", "m7", "m8"), texts(history));
        assertEquals(0.0, fallbacks());
    }
}
