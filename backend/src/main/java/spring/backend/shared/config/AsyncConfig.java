package spring.backend.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Virtual Thread가 활성화되면 Spring Boot가 자동으로
 * VT 기반 SimpleAsyncTaskExecutor를 제공하므로 수동 Executor 불필요.
 *
 * @see application.yml: spring.threads.virtual.enabled=true
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
