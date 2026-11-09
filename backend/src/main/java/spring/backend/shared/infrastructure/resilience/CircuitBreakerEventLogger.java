package spring.backend.shared.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 서킷 상태 전이마다 구조화 로그 한 줄 -> Loki 가 [CB_STATE] 줄을 세어 전이 횟수를 센다
// 집계 지표(resilience4j_circuitbreaker_state)는 현재 상태만 있어 언제 바뀌었는지 남지 않는다
@Slf4j
@Component
public class CircuitBreakerEventLogger {

    // 같은 인스턴스에 소비자를 두 번 달면 전이 한 번에 로그가 두 줄 나간다
    private final Set<String> registered = ConcurrentHashMap.newKeySet();

    public CircuitBreakerEventLogger(CircuitBreakerRegistry registry) {
        // 이벤트 구독을 먼저 걸고 기존 인스턴스를 훑는다 -> 그 사이에 추가된 인스턴스를 놓치지 않는다
        registry.getEventPublisher().onEntryAdded(event -> register(event.getAddedEntry()));
        registry.getAllCircuitBreakers().forEach(this::register);
    }

    private void register(CircuitBreaker circuitBreaker) {
        if (!registered.add(circuitBreaker.getName())) {
            return;
        }
        circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.info("[CB_STATE] name={} from={} to={}",
                        event.getCircuitBreakerName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()));
    }
}
