package spring.backend.shared.test;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * k6 스레드 풀 부하 테스트용 컨트롤러 (dev 환경 한정). POST /api/test/load 를 호출시 async-chat 스레드 풀에 작업 제출 (스레드 점유 시작) HTTP 200 즉시 반환 —
 * /api/chat/message 와 동일한 패턴 백그라운드에서 {@code seconds}초 후 스레드 해방 스레드 풀 한계(max=50, queue=100): 동시 요청 51개 이상 → 큐 진입, 151개
 * 이상 → RejectedExecutionException → HTTP 500
 */
@Profile("dev")
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class ThreadPoolTestController {

    private final ThreadPoolTestService threadPoolTestService;

    /**
     * @param seconds 스레드를 점유할 시간(초). 기본값 8초.
     */
    @PostMapping("/load")
    public ResponseEntity<Void> triggerLoad(
            @RequestParam(defaultValue = "8") int seconds) {
        threadPoolTestService.holdThread(seconds);
        return ResponseEntity.ok().build();
    }

    /**
     * Redis Connection Pool 부하 테스트 엔드포인트. Auth Redis + Chat Redis에 SET/GET/DELETE를 ops 횟수만큼 반복.
     *
     * @param ops 반복 횟수 (기본값 10)
     * @return 소요 시간(ms)
     */
    @PostMapping("/redis-stress")
    public ResponseEntity<String> redisStress(
            @RequestParam(defaultValue = "10") int ops) {
        long elapsed = threadPoolTestService.redisStress(ops);
        return ResponseEntity.ok(elapsed + "ms");
    }
}
