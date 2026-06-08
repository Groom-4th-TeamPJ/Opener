package spring.backend.shared.test;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
     * 요청(호출) 스레드를 동기로 점유 — @Async 없음 holdThread 와 달리 호출 스레드가 직접 슬립하므로 VT off + Tomcat 스레드 풀 고정 시 요청 스레드
     * 고갈을 재현하고, VT on 시에는 연결당 스레드 점유가 사라지는 차이를 A/B 로 비교
     */
    public void holdThreadSync(int durationSeconds) {
        log.debug("[k6-sync] {} — {}초 동기 슬립", Thread.currentThread().getName(), durationSeconds);
        try {
            Thread.sleep(durationSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 가상 외부 의존성 호출 — 외부 네트워크 없이 서킷브레이커 동작을 재현 delayMs 만큼 지연 후 fail=true 면 예외 발생 (느린 실패). 실패율이 임계치를 넘으면 서킷이
     * OPEN 되어 이후 호출은 fallback 으로 즉시 차단(fast-fail)
     */
    @CircuitBreaker(name = "test-circuit", fallbackMethod = "unstableFallback")
    public String unstableCall(boolean fail, long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (fail) {
            throw new RuntimeException("simulated external failure");
        }
        return "ok";
    }

    // 서킷 OPEN 시 CallNotPermittedException 등으로 진입 — 지연 없이 즉시 반환(fast-fail)
    public String unstableFallback(boolean fail, long delayMs, Throwable t) {
        return "fallback:" + t.getClass().getSimpleName();
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

    /**
     * DB + Redis 복합 부하 테스트 실제 채팅 서비스 패턴 모사: Redis 세션 조회 → DB 쿼리 → Redis 결과 저장
     *
     * @param ops 반복 횟수
     * @return 결과 Map (dbElapsed, redisElapsed, totalElapsed)
     */
    public Map<String, Long> combinedStress(int ops) {
        long start = System.currentTimeMillis();
        long redisElapsed = 0L;
        long dbElapsed = 0L;
        // Hash Tag 으로 같은 슬롯 보장 -> Cluster 멀티키 CROSSSLOT 회피
        String keyPrefix = "combined-test:{stress}:" + Thread.currentThread().getName() + ":";

        for (int i = 0; i < ops; i++) {
            String key = keyPrefix + i;

            // 1단계 Redis 세션 조회 모사
            long r1 = System.currentTimeMillis();
            chatRedisTemplate.opsForValue().set(key, "session" + i);
            chatRedisTemplate.opsForValue().get(key);
            redisElapsed += System.currentTimeMillis() - r1;

            // 2단계 DB 쿼리 모사 -> HikariCP 커넥션 점유
            long d1 = System.currentTimeMillis();
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            dbElapsed += System.currentTimeMillis() - d1;

            // 3단계 Redis 결과 저장 모사
            long r2 = System.currentTimeMillis();
            chatRedisTemplate.opsForValue().set(key, "result" + i);
            chatRedisTemplate.delete(key);
            redisElapsed += System.currentTimeMillis() - r2;
        }

        long total = System.currentTimeMillis() - start;
        return Map.of(
                "dbElapsed", dbElapsed,
                "redisElapsed", redisElapsed,
                "totalElapsed", total
        );
    }
}
