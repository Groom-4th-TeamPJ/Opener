package spring.backend.shared.test;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * k6 Virtual Thread 부하 테스트용 컨트롤러 (dev 환경 한정) VT 환경에서 병목이 되는 하위 리소스(DB, Redis) 경합을 측정
 */
@Profile("dev")
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class ThreadPoolTestController {

    private final ThreadPoolTestService threadPoolTestService;

    /**
     * VT 슬립 테스트 — 동시 VT 수천 개에서도 거부 없이 처리되는지 검증
     *
     * @param seconds 슬립 시간(초), 기본값 8
     */
    @PostMapping("/load")
    public ResponseEntity<Void> triggerLoad(
            @RequestParam(defaultValue = "8") int seconds) {
        threadPoolTestService.holdThread(seconds);
        return ResponseEntity.ok().build();
    }

    /**
     * Redis 커넥션 풀 부하 테스트 Auth Redis + Chat Redis에 SET/GET/DELETE 반복
     *
     * @param ops 반복 횟수, 기본값 10
     * @return 소요 시간(ms)
     */
    @PostMapping("/redis-stress")
    public ResponseEntity<String> redisStress(
            @RequestParam(defaultValue = "10") int ops) {
        long elapsed = threadPoolTestService.redisStress(ops);
        return ResponseEntity.ok(elapsed + "ms");
    }

    /**
     * DB 커넥션 풀 부하 테스트 pg_sleep으로 HikariCP 커넥션 점유, VT 동시 요청 시 pool exhaustion 재현
     *
     * @param holdSeconds 커넥션 점유 시간(초), 기본값 2
     * @return 소요 시간(ms)
     */
    @PostMapping("/db-stress")
    public ResponseEntity<String> dbStress(
            @RequestParam(defaultValue = "2") int holdSeconds) {
        long elapsed = threadPoolTestService.dbStress(holdSeconds);
        return ResponseEntity.ok(elapsed + "ms");
    }

    /**
     * DB + Redis 복합 부하 테스트 실제 채팅 서비스의 리소스 사용 패턴 모사
     *
     * @param ops 반복 횟수, 기본값 5
     * @return 리소스별 소요 시간
     */
    @PostMapping("/combined-stress")
    public ResponseEntity<Map<String, Long>> combinedStress(
            @RequestParam(defaultValue = "5") int ops) {
        Map<String, Long> result = threadPoolTestService.combinedStress(ops);
        return ResponseEntity.ok(result);
    }
}
