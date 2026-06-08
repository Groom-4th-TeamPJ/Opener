package spring.backend.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;


// @EnableRetry -> @Retryable 활성화, 일시적 DB 오류를 재시도로 흡수해 단발 장애에 견고
@Configuration
@EnableRetry
public class RetryConfig {
}
