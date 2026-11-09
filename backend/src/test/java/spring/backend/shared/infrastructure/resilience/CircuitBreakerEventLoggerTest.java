package spring.backend.shared.infrastructure.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

// 상태 전이 소비자가 없으면 서킷이 열리고 닫힌 시각이 어디에도 남지 않는다 -> 대시보드가 fallback 경고 로그를 전이로 착각해 센다
class CircuitBreakerEventLoggerTest {

    private final Logger target = (Logger) LoggerFactory.getLogger(CircuitBreakerEventLogger.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attach() {
        appender.start();
        target.addAppender(appender);
    }

    @AfterEach
    void detach() {
        target.detachAppender(appender);
    }

    @Test
    @DisplayName("이미 등록된 인스턴스 둘이 OPEN 으로 바뀌면 인스턴스마다 전이 로그 한 줄이 남는다")
    void stateTransition_등록된인스턴스_로그한줄씩() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker chat = registry.circuitBreaker("llm-chat");
        CircuitBreaker summary = registry.circuitBreaker("llm-summary");
        new CircuitBreakerEventLogger(registry);

        chat.transitionToOpenState();
        summary.transitionToOpenState();

        assertEquals(List.of(
                "[CB_STATE] name=llm-chat from=CLOSED to=OPEN",
                "[CB_STATE] name=llm-summary from=CLOSED to=OPEN"), messages());
    }

    @Test
    @DisplayName("로거 생성 뒤 새로 등록된 인스턴스도 전이 로그를 남긴다")
    void stateTransition_나중에등록된인스턴스_로그한줄() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        new CircuitBreakerEventLogger(registry);

        registry.circuitBreaker("redis-chat").transitionToOpenState();

        assertEquals(List.of("[CB_STATE] name=redis-chat from=CLOSED to=OPEN"), messages());
    }

    @Test
    @DisplayName("같은 인스턴스를 다시 조회해도 소비자가 중복 등록되지 않는다")
    void stateTransition_재조회_로그한줄() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        registry.circuitBreaker("llm-rag");
        new CircuitBreakerEventLogger(registry);

        registry.circuitBreaker("llm-rag").transitionToOpenState();

        assertEquals(1, messages().size());
    }

    private List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
