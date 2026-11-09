package spring.backend.domain.chat.service.impl;

import io.lettuce.core.RedisClient;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.nio.charset.StandardCharsets;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 선점 스크립트의 Lua 의미를 실제 Redis 에서 확인한다 -> Mockito 로는 HSETNX · EXPIRE · TTL 의 실제 동작을 볼 수 없다
// REDIS_TEST_PORT 가 있을 때만 돈다: docker run -d --rm -p 16399:6379 --name r115-t1-redis redis:7
// 실행: REDIS_TEST_PORT=16399 ./gradlew cleanTest test --tests '*ChatSessionClaimLuaTest'
@EnabledIfEnvironmentVariable(named = "REDIS_TEST_PORT", matches = ".+")
class ChatSessionClaimLuaTest {

    private static final String TTL_SECONDS = "3600";
    private static final String CREATED_AT = "1700000000000";
    // 스크립트 반환 코드 -> 1 = 내 세션 · 0 = 다른 사용자 소유 · 2 = 키는 있는데 userId 없음
    private static final long MINE = 1L;
    private static final long OTHERS = 0L;
    private static final long NO_OWNER = 2L;

    private static RedisClient client;
    private static StatefulRedisConnection<String, String> connection;
    private static String script;

    private RedisCommands<String, String> redis;
    private String key;

    @BeforeAll
    static void connect() throws Exception {
        client = RedisClient.create("redis://localhost:" + System.getenv("REDIS_TEST_PORT"));
        connection = client.connect();
        script = new ClassPathResource("scripts/chat-session-claim.lua").getContentAsString(StandardCharsets.UTF_8);
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
        key = "chat:{lua-test-" + UUID.randomUUID() + "}:session";
    }

    @AfterEach
    void cleanUp() {
        redis.del(key);
    }

    private long claim(String userId) {
        return redis.eval(script, ScriptOutputType.INTEGER, new String[]{key}, userId, TTL_SECONDS, CREATED_AT);
    }

    @Test
    @DisplayName("없는 키는 선점되고 소유자·생성시각·TTL 이 함께 기록된다")
    void claim_신규키_선점하고TTL이걸린다() {
        assertEquals(MINE, claim("user-a"));

        assertEquals("user-a", redis.hget(key, "userId"));
        assertEquals(CREATED_AT, redis.hget(key, "createdAt"));
        long ttl = redis.ttl(key);
        assertTrue(ttl > 0 && ttl <= 3600, "TTL 이 걸리지 않음: " + ttl);
    }

    @Test
    @DisplayName("같은 소유자가 다시 선점하면 1 을 돌려주고 TTL 과 생성시각을 건드리지 않는다")
    void claim_같은소유자_통과하고기존값을유지한다() {
        claim("user-a");
        redis.expire(key, 100);

        assertEquals(MINE, claim("user-a"));

        long ttl = redis.ttl(key);
        assertTrue(ttl > 0 && ttl <= 100, "재연결이 TTL 을 되돌림: " + ttl);
        assertEquals(CREATED_AT, redis.hget(key, "createdAt"));
    }

    @Test
    @DisplayName("다른 소유자가 선점하면 0 을 돌려주고 기존 소유자를 바꾸지 않는다")
    void claim_다른소유자_거부하고소유자를유지한다() {
        claim("user-a");

        assertEquals(OTHERS, claim("user-b"));

        assertEquals("user-a", redis.hget(key, "userId"));
    }

    @Test
    @DisplayName("키는 있는데 userId 가 없으면 전용 코드를 돌려주고 소유자가 되지 못한다")
    void claim_userId없는기존키_failClosed코드를돌려준다() {
        redis.hset(key, "createdAt", "1");

        assertEquals(NO_OWNER, claim("user-b"));

        // HSETNX 만 쓰면 이 요청이 남이 만든 키의 소유자가 된다
        assertNull(redis.hget(key, "userId"));
    }

    @Test
    @DisplayName("TTL 없이 남은 내 키는 선점 때 TTL 을 되찾는다")
    void claim_TTL없는내키_TTL을복구한다() {
        redis.hset(key, "userId", "user-a");
        assertEquals(-1L, redis.ttl(key));

        assertEquals(MINE, claim("user-a"));

        long ttl = redis.ttl(key);
        assertTrue(ttl > 0 && ttl <= 3600, "TTL 이 복구되지 않음: " + ttl);
    }
}
