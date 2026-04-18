package spring.backend.shared.test;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * k6 Virtual Thread 부하 테스트 전용 서비스 (dev 환경 한정) VT 환경에서는 스레드 풀 고갈이 아닌 하위 리소스(DB 커넥션 풀, Redis 커넥션 풀)가 병목 해당 병목 지점들을 k6로
 * 측정하기 위한 엔드포인트 제공
 */
@Slf4j
@Profile("dev")
@Service
public class ThreadPoolTestService {

    private final StringRedisTemplate authRedisTemplate;
    private final StringRedisTemplate chatRedisTemplate;
    private final JdbcTemplate jdbcTemplate;

    public ThreadPoolTestService(
            StringRedisTemplate authRedisTemplate,
            @Qualifier("chatRedisTemplate") StringRedisTemplate chatRedisTemplate,
            JdbcTemplate jdbcTemplate) {
        this.authRedisTemplate = authRedisTemplate;
        this.chatRedisTemplate = chatRedisTemplate;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * VT 하나를 {@code durationSeconds}초 동안 슬립 VT에서는 sleep이 platform thread를 점유하지 않으므로 수천 개가 동시 슬립해도 OS 스레드 고갈 없음을 검증
     */
    @Async
    public void holdThread(int durationSeconds) {
        log.debug("[k6-vt] {} — {}초 슬립 시작", Thread.currentThread().getName(), durationSeconds);
        try {
            Thread.sleep(durationSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.debug("[k6-vt] {} — 슬립 종료", Thread.currentThread().getName());
    }

    /**
     * Auth Redis(Standalone) + Chat Redis(Cluster)에 SET/GET/DELETE 반복 Redis 커넥션 풀 경합 측정용
     *
     * @param ops 반복 횟수
     * @return 소요 시간(ms)
     */
    public long redisStress(int ops) {
        long start = System.currentTimeMillis();
        String keyPrefix = "pool-test:" + Thread.currentThread().getName() + ":";

        for (int i = 0; i < ops; i++) {
            String key = keyPrefix + i;

            // Auth Redis (Standalone)
            authRedisTemplate.opsForValue().set(key, "v" + i);
            authRedisTemplate.opsForValue().get(key);
            authRedisTemplate.delete(key);

            // Chat Redis (Cluster) — Hash Tag으로 같은 슬롯 보장
            String chatKey = "pool-test:{stress}:" + Thread.currentThread().getName() + ":" + i;
            chatRedisTemplate.opsForValue().set(chatKey, "v" + i);
            chatRedisTemplate.opsForValue().get(chatKey);
            chatRedisTemplate.delete(chatKey);
        }

        return System.currentTimeMillis() - start;
    }

    /**
     * HikariCP 커넥션 풀 경합 테스트 pg_sleep으로 커넥션을 holdSeconds초 동안 점유 VT 다수 동시 요청 시 커넥션 대기(pool exhaustion) 재현
     *
     * @param holdSeconds 커넥션 점유 시간(초)
     * @return 소요 시간(ms)
     */
    public long dbStress(int holdSeconds) {
        long start = System.currentTimeMillis();
        log.debug("[k6-vt] DB 커넥션 점유 시작 - thread: {}, hold: {}s",
                Thread.currentThread().getName(), holdSeconds);

        jdbcTemplate.execute("SELECT pg_sleep(" + holdSeconds + ")");

        long elapsed = System.currentTimeMillis() - start;
        log.debug("[k6-vt] DB 커넥션 점유 종료 - thread: {}, elapsed: {}ms",
                Thread.currentThread().getName(), elapsed);
        return elapsed;
    }

    // TODO : combinedStress 구현 (이건 테스트 시나리오 복잡할 것 같아 추후 구체적으로 작성하기로..)

    /**
     * DB + Redis 복합 부하 테스트 실제 채팅 서비스 패턴 모사: Redis 세션 조회 → DB 쿼리 → Redis 결과 저장
     *
     * @param ops 반복 횟수
     * @return 결과 Map (dbElapsed, redisElapsed, totalElapsed)
     */
    public Map<String, Long> combinedStress(int ops) {
        return Map.of();
    }
}
