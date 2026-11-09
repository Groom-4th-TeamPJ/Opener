package spring.backend.shared.infrastructure.messaging.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.boot.retry.RetryPolicySettings;
import org.springframework.stereotype.Component;

// 리스너 재시도 정책에서 재시도 불가 예외를 뺀다
// Boot 의 기본 정책은 모든 예외를 다시 시도해 AmqpRejectAndDontRequeueException 을 던져도 재시도 횟수만큼 처리가 반복된다
// 최대 횟수와 대기 시간은 spring.rabbitmq.listener.simple.retry.* 에서 이미 채워진 설정을 그대로 쓴다
// RabbitMQConfig 를 건드리지 않고 SimpleRabbitListenerContainerFactoryConfigurer 가 이 빈을 주입받아 적용한다
@Component
public class RabbitListenerRetryPolicyCustomizer implements RabbitListenerRetrySettingsCustomizer {

    @Override
    public void customize(RetryPolicySettings settings) {
        // 기존 제외 목록을 지우지 않고 덧붙인다
        List<Class<? extends Throwable>> excludes = new ArrayList<>(settings.getExceptionExcludes());
        // 영구 실패 -> 컨슈머가 던지는 거절 예외(원인을 단 채 DLQ 로 간다)
        excludes.add(AmqpRejectAndDontRequeueException.class);
        // 메시지 본문을 변환하지 못한 실패 -> 다시 읽어도 같은 결과
        excludes.add(MessageConversionException.class);
        settings.setExceptionExcludes(excludes);
    }
}
