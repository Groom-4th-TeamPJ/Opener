package spring.backend.shared.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Resilience4j 의 ReactorOnClasspathCondition 이 요구하는 클래스가 실제로 있는지 확인
// 이 클래스가 없으면 ReactorCircuitBreakerAspectExt 빈이 조용히 등록되지 않고
// Flux 반환 메서드의 @CircuitBreaker 는 조립 시간만 감싼다
class ResilienceReactorIntegrationTest {

    @Test
    @DisplayName("리액터 서킷 확장에 필요한 AbstractSubscriber 가 클래스패스에 있다")
    void 클래스패스_리액터확장_존재한다() {
        assertDoesNotThrow(() ->
                Class.forName("io.github.resilience4j.reactor.AbstractSubscriber"));
    }

    @Test
    @DisplayName("Flux 구독 중 발생한 오류가 서킷 실패로 집계된다")
    void 서킷_구독중오류_실패로집계된다() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50)
                .build();
        CircuitBreaker breaker = CircuitBreaker.of("reactor-probe", config);

        // 첫 청크는 정상 도착하고 그 뒤에 오류 -> 조립 시점에는 성공, 구독 중에 실패
        Flux<String> failing = Flux.concat(
                        Flux.just("token"),
                        Flux.error(new IllegalStateException("stream broke")))
                .transformDeferred(CircuitBreakerOperator.of(breaker));

        StepVerifier.create(failing)
                .expectNext("token")
                .expectError(IllegalStateException.class)
                .verify(Duration.ofSeconds(2));

        assertEquals(1, breaker.getMetrics().getNumberOfFailedCalls());
    }
}
