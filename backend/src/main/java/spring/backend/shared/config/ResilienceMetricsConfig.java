package spring.backend.shared.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * resilience4j 서킷브레이커 메트릭을 Micrometer 에 수동 바인딩 Spring Boot 4 + resilience4j-spring-boot3 조합에서 자동 바인딩이
 * 동작하지 않아 resilience4j_circuitbreaker_* 메트릭이 Prometheus 에 노출되지 않는 문제를 해결
 */
@Configuration
public class ResilienceMetricsConfig {

    @Bean
    public TaggedCircuitBreakerMetrics circuitBreakerMetrics(
            CircuitBreakerRegistry circuitBreakerRegistry, MeterRegistry meterRegistry) {
        TaggedCircuitBreakerMetrics metrics =
                TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry);
        metrics.bindTo(meterRegistry);
        return metrics;
    }
}
