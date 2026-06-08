package spring.backend.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Virtual Thread가 활성화되면 Spring Boot가 자동으로
 * VT 기반 SimpleAsyncTaskExecutor를 제공하므로 수동 Executor 불필요.
 *
 * @see application.yml: spring.threads.virtual.enabled=true
 */
// @EnableAsync -> @Async 메서드를 별도 스레드로 분리, 호출자가 결과를 기다리지 않게 함
// Executor 빈 미정의 -> Virtual Thread 활성화 시 Spring 이 VT 기반 Executor 자동 제공하므로 수동 설정 불필요
@Configuration
@EnableAsync
public class AsyncConfig {
}
