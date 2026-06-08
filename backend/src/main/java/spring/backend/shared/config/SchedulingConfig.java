package spring.backend.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EnableScheduling -> @Scheduled 활성화, SSE Heartbeat·토큰 발급 등 주기 작업 동작
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
