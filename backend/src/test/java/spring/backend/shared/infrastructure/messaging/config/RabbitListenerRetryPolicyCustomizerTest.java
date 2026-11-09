package spring.backend.shared.infrastructure.messaging.config;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.boot.retry.RetryPolicySettings;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

// 재시도 불가 예외를 던져도 재시도가 건너뛰어지지 않았다 -> Boot 의 리스너 재시도 정책은 모든 예외를 다시 시도한다
// 커스터마이저를 적용한 정책으로 실제 RetryTemplate 을 돌려 호출 횟수를 센다
class RabbitListenerRetryPolicyCustomizerTest {

    private static final long MAX_RETRIES = 3;

    private RetryTemplate templateWithCustomizer() {
        RetryPolicySettings settings = new RetryPolicySettings();
        settings.setMaxRetries(MAX_RETRIES);
        settings.setDelay(Duration.ofMillis(1));
        new RabbitListenerRetryPolicyCustomizer().customize(settings);
        return new RetryTemplate(settings.createRetryPolicy());
    }

    private int attemptsUntilGiveUp(Throwable failure) {
        AtomicInteger attempts = new AtomicInteger();
        assertThrows(RetryException.class, () -> templateWithCustomizer().execute(() -> {
            attempts.incrementAndGet();
            throw failure;
        }));
        return attempts.get();
    }

    @Test
    @DisplayName("AmqpRejectAndDontRequeueException 은 재시도 없이 한 번만 실행된다")
    void retryPolicy_거절예외_재시도하지않는다() {
        assertEquals(1, attemptsUntilGiveUp(new AmqpRejectAndDontRequeueException("재시도 불가")));
    }

    @Test
    @DisplayName("리스너 어댑터가 감싼 거절 예외도 원인을 따라가 재시도하지 않는다")
    void retryPolicy_감싼거절예외_재시도하지않는다() {
        // 리스너 안에서 던진 예외는 ListenerExecutionFailedException 의 원인으로 정책에 도착한다
        Throwable wrapped = new ListenerExecutionFailedException("리스너 실패",
                new AmqpRejectAndDontRequeueException("재시도 불가"));

        assertEquals(1, attemptsUntilGiveUp(wrapped));
    }

    @Test
    @DisplayName("메시지 변환 실패는 재시도하지 않는다")
    void retryPolicy_변환실패_재시도하지않는다() {
        assertEquals(1, attemptsUntilGiveUp(new MessageConversionException("역직렬화 실패")));
    }

    @Test
    @DisplayName("일반 예외는 최대 재시도 횟수까지 다시 실행된다")
    void retryPolicy_일반예외_최대횟수까지재시도한다() {
        // 일시 장애(Redis · DB)는 재시도로 회복될 수 있어야 한다
        assertEquals(1 + MAX_RETRIES, attemptsUntilGiveUp(new RuntimeException("일시 장애")));
    }
}
