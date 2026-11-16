package spring.backend.domain.chat.service.impl;

import io.lettuce.core.RedisClient;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 저장 스크립트의 Lua 의미를 실제 Redis 에서 확인한다 -> Mockito 로는 RPUSH 반환 길이와 EXPIRE 효과를 볼 수 없다
// REDIS_TEST_PORT 가 있을 때만 돈다: docker run -d --rm -p 16399:6379 --name r125-t1-redis redis:7
// 실행: REDIS_TEST_PORT=16399 ./gradlew cleanTest test --tests '*ChatMessageSaveLuaTest'
@EnabledIfEnvironmentVariable(named = "REDIS_TEST_PORT", matches = ".+")
class ChatMessageSaveLuaTest {

    private static final String TTL_SECONDS = "3600";

    private static RedisClient client;
    private static StatefulRedisConnection<String, String> connection;
    private static String script;

    private RedisCommands<String, String> redis;
    private String messageKey;
    private String sessionKey;

    @BeforeAll
    static void connect() throws Exception {
        client = RedisClient.create("redis://localhost:" + System.getenv("REDIS_TEST_PORT"));
        connection = client.connect();
        script = new ClassPathResource("scripts/chat-message-save.lua").getContentAsString(StandardCharsets.UTF_8);
    }

    @AfterAll
    static void disconnect() {
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }

    @BeforeEach
    void setUp() {
        redis = connection.sync();
        String id = "save-lua-test-" + UUID.randomUUID();
        messageKey = "chat:{" + id + "}:messages";
        sessionKey = "chat:{" + id + "}:session";
    }

    @AfterEach
    void cleanUp() {
        redis.del(messageKey, sessionKey);
    }

    private long save(String json) {
        return redis.eval(script, ScriptOutputType.INTEGER, new String[]{messageKey, sessionKey}, json, TTL_SECONDS);
    }

    @Test
    @DisplayName("저장할 때마다 push 후의 목록 길이를 돌려주고 메시지는 들어온 순서로 쌓인다")
    void save_세번저장_길이1_2_3을돌려준다() {
        assertEquals(1L, save("{\"m\":\"a\"}"));
        assertEquals(2L, save("{\"m\":\"b\"}"));
        assertEquals(3L, save("{\"m\":\"c\"}"));

        // 순서가 뒤바뀌면 히스토리의 절대 인덱스 범위가 다른 메시지를 가리킨다
        assertEquals(List.of("{\"m\":\"a\"}", "{\"m\":\"b\"}", "{\"m\":\"c\"}"), redis.lrange(messageKey, 0, -1));
    }

    @Test
    @DisplayName("저장하면 메시지 키와 세션 키 둘 다 TTL 이 3590~3600 초로 잡힌다")
    void save_세션키가있음_두키모두TTL이걸린다() {
        redis.hset(sessionKey, "userId", "user-a");

        save("{\"m\":\"a\"}");

        long messageTtl = redis.ttl(messageKey);
        long sessionTtl = redis.ttl(sessionKey);
        assertTrue(messageTtl >= 3590 && messageTtl <= 3600, "메시지 키 TTL: " + messageTtl);
        assertTrue(sessionTtl >= 3590 && sessionTtl <= 3600, "세션 키 TTL: " + sessionTtl);
    }

    @Test
    @DisplayName("TTL 이 10 초 남은 세션 키는 저장 한 번에 3600 초 근처로 되돌아온다")
    void save_세션TTL10초_갱신된다() {
        redis.hset(sessionKey, "userId", "user-a");
        redis.expire(sessionKey, 10);

        save("{\"m\":\"a\"}");

        // 갱신이 빠지면 대화 중인 세션이 마지막 저장 후 10 초 뒤에 만료된다
        long sessionTtl = redis.ttl(sessionKey);
        assertTrue(sessionTtl >= 3590 && sessionTtl <= 3600, "세션 키 TTL: " + sessionTtl);
    }

    @Test
    @DisplayName("세션 키가 없어도 저장은 성공하고 세션 키를 새로 만들지 않는다")
    void save_세션키없음_저장은성공하고세션키를만들지않는다() {
        assertEquals(1L, save("{\"m\":\"a\"}"));

        // 소유자 없는 세션 키가 생기면 선점 스크립트가 그 키를 가로채지 않고 거부한다(코드 2)
        assertEquals(0L, redis.exists(sessionKey));
    }
}
