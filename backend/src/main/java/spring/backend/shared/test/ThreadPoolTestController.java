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
     * 동기 점유 부하 — @Async 없이 요청 스레드가 직접 슬립 VT on/off + Tomcat 스레드 풀 크기에 따라 동시 처리량이 어떻게 달라지는지 A/B 측정용
     *
     * @param seconds 요청 스레드 점유 시간(초), 기본값 2
     */
    @PostMapping("/load-sync")
    public ResponseEntity<Void> triggerLoadSync(
            @RequestParam(defaultValue = "2") int seconds) {
        threadPoolTestService.holdThreadSync(seconds);
        return ResponseEntity.ok().build();
    }

    /**
     * 가상 장애 주입 — 외부 호출 없이 서킷브레이커 fast-fail 측정용 fail=true + delayMs 지연으로 느린 실패를 만들면 실패율 임계치 초과 시 서킷 OPEN,
     * 이후 호출은 fallback 으로 즉시 차단됨
     *
     * @param fail 예외 발생 여부, 기본 true
     * @param delayMs 응답 전 지연(ms), 기본 3000
     */
    @PostMapping("/circuit")
    public ResponseEntity<String> circuit(
            @RequestParam(defaultValue = "true") boolean fail,
            @RequestParam(defaultValue = "3000") long delayMs) {
        return ResponseEntity.ok(threadPoolTestService.unstableCall(fail, delayMs));
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
