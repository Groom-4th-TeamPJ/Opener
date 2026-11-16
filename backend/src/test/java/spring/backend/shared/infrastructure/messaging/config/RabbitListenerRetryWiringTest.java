package spring.backend.shared.infrastructure.messaging.config;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import spring.backend.domain.chat.dto.message_dto.ChatSummaryEvent;
import spring.backend.domain.chat.messaging.ChatSummaryConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 실제 RabbitMQConfig 가 만든 rabbitListenerContainerFactory 의 재시도에서 거절 예외가 제외되는지 확인한다
// 같은 이름의 빈이 Boot 자동 구성 팩토리를 대체하므로 Boot 팩토리만 보는 검증은 이 팩토리의 동작을 보장하지 않는다
// RabbitMQConfig 가 Boot 설정자를 거쳐야 RabbitListenerRetrySettingsCustomizer 빈이 이 팩토리에 닿는다
class RabbitListenerRetryWiringTest {

    private static final long MAX_RETRIES = 3;

    @Test
    @DisplayName("RabbitMQConfig 의 리스너 팩토리는 거절 예외를 재시도하지 않고 일반 예외는 최대 횟수까지 재시도한다")
    void realFactory_거절예외_재시도하지않고일반예외는재시도한다() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                .withUserConfiguration(RabbitMQConfig.class, RabbitListenerRetryPolicyCustomizer.class)
                .withPropertyValues(
                        "spring.rabbitmq.listener.simple.retry.enabled=true",
                        "spring.rabbitmq.listener.simple.retry.max-retries=" + MAX_RETRIES,
                        "spring.rabbitmq.listener.simple.retry.initial-interval=1ms")
                .run(context -> {
                    SimpleRabbitListenerContainerFactory factory =
                            context.getBean("rabbitListenerContainerFactory", SimpleRabbitListenerContainerFactory.class);
                    // Boot 의 자동 구성 팩토리가 아니라 RabbitMQConfig 가 만든 팩토리인지 확인한다
                    assertTrue(factory.getClass() == SimpleRabbitListenerContainerFactory.class
                            && context.getBeansOfType(SimpleRabbitListenerContainerFactory.class).size() == 1);
                    assertEquals(false, ReflectionTestUtils.getField(factory, "defaultRequeueRejected"));

                    RetryTemplate template = retryTemplateOf(factory);

                    assertEquals(1, attemptsUntilGiveUp(template, new AmqpRejectAndDontRequeueException("재시도 불가")));
                    assertEquals(1 + MAX_RETRIES, attemptsUntilGiveUp(template, new RuntimeException("일시 장애")));
                });
    }

    @Test
    @DisplayName("요약 큐는 DLX 로 dead-letter 하고 DLQ 와 바인딩이 선언된다")
    void summaryQueue_DLX연결_DLQ와바인딩선언() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                .withUserConfiguration(RabbitMQConfig.class, RabbitListenerRetryPolicyCustomizer.class)
                .run(context -> {
                    Queue queue = queueNamed(context, "chat.message.summary.queue");
                    assertEquals(true, queue.isDurable());
                    // 실패한 요약이 사라지지 않고 요약 전용 DLQ 에 쌓여야 원인 분석과 재처리가 된다
                    assertEquals("chat.message.dlx", queue.getArguments().get("x-dead-letter-exchange"));
                    assertEquals("chat.message.summary.dlq", queue.getArguments().get("x-dead-letter-routing-key"));

                    assertEquals(true, queueNamed(context, "chat.message.summary.dlq").isDurable());

                    Binding main = bindingTo(context, "chat.message.summary.queue");
                    assertEquals("chat.message.exchange", main.getExchange());
                    assertEquals("chat.message.summary", main.getRoutingKey());
                    Binding dead = bindingTo(context, "chat.message.summary.dlq");
                    assertEquals("chat.message.dlx", dead.getExchange());
                    assertEquals("chat.message.summary.dlq", dead.getRoutingKey());
                });
    }

    // 리스너가 별도 컨테이너 팩토리를 지정하면 위 재시도 제외 규칙이 닿지 않는다
    @Test
    @DisplayName("요약 리스너는 요약 큐를 읽고 별도 팩토리 없이 공유 팩토리의 재시도 규칙을 쓴다")
    void summaryListener_공유팩토리_재시도제외규칙이닿는다() throws Exception {
        RabbitListener listener = ChatSummaryConsumer.class
                .getMethod("handleSummaryEvent", ChatSummaryEvent.class)
                .getAnnotation(RabbitListener.class);

        assertNotNull(listener, "요약 소비자에 @RabbitListener 가 없음");
        assertEquals(List.of("chat.message.summary.queue"), List.of(listener.queues()));
        assertEquals("", listener.containerFactory());
        assertEquals("", listener.errorHandler());
    }

    private Queue queueNamed(AssertableApplicationContext context, String name) {
        return context.getBeansOfType(Queue.class).values().stream()
                .filter(queue -> queue.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("큐 선언 없음: " + name));
    }

    private Binding bindingTo(AssertableApplicationContext context, String queueName) {
        return context.getBeansOfType(Binding.class).values().stream()
                .filter(binding -> binding.getDestination().equals(queueName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("바인딩 선언 없음: " + queueName));
    }

    // 팩토리의 advice 체인 -> StatelessRetryOperationsInterceptor -> RetryTemplate (필드는 private)
    private RetryTemplate retryTemplateOf(SimpleRabbitListenerContainerFactory factory) {
        Advice[] chain = factory.getAdviceChain();
        assertNotNull(chain, "재시도 설정이 켜졌는데 어드바이스 체인이 없음");
        assertTrue(chain.length > 0);
        return (RetryTemplate) ReflectionTestUtils.getField(chain[0], "retryOperations");
    }

    private int attemptsUntilGiveUp(RetryTemplate template, Throwable failure) {
        AtomicInteger attempts = new AtomicInteger();
        assertThrows(RetryException.class, () -> template.execute(() -> {
            attempts.incrementAndGet();
            throw failure;
        }));
        return attempts.get();
    }
}
