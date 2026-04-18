package spring.backend.shared.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * k6 스레드 풀 부하 테스트 전용 서비스 (dev 환경 한정) 실제 LLM 호출 없이 async-chat 스레드 풀을 점유하여 /api/chat/message 와 동일한 스레드 점유 패턴을 재현
 */
@Slf4j
@Profile("dev")
@Service
public class ThreadPoolTestService {

    private final StringRedisTemplate authRedisTemplate;
    private final StringRedisTemplate chatRedisTemplate;

    public ThreadPoolTestService(
            StringRedisTemplate authRedisTemplate,
            @Qualifier("chatRedisTemplate") StringRedisTemplate chatRedisTemplate) {
        this.authRedisTemplate = authRedisTemplate;
        this.chatRedisTemplate = chatRedisTemplate;
    }

    /**
     * async-chat 스레드 풀의 스레드 하나를 {@code durationSeconds}초 동안 점유 호출 즉시 반환 (비동기) 스레드 풀 내부에서 슬립상태 대기
     */
    @Async
    public void holdThread(int durationSeconds) {
        log.debug("[k6-test] {} — {}초 점유 시작", Thread.currentThread().getName(), durationSeconds);
        try {
            Thread.sleep(durationSeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.debug("[k6-test] {} — 점유 종료", Thread.currentThread().getName());
    }

    /**
     * Auth Redis(Standalone)와 Chat Redis(Cluster)에 SET/GET을 반복하여 Connection Pool 효과를 측정
     *
     * @param ops SET/GET 반복 횟수
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
}
