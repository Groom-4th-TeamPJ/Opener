package spring.backend.shared.infrastructure.messaging.config;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

// 저장 큐는 이벤트 하나에 요약 LLM 호출이 붙어 소비자 하나로는 발행 속도(초당 9.1건)를 못 따라간다
// application-dev.yml 의 동시 소비자 수가 RabbitMQConfig 의 리스너 팩토리까지 닿는지 실제 설정 파일로 확인한다
// 같은 팩토리를 저장 리스너와 요약 리스너가 함께 쓴다
class RabbitListenerConcurrencyWiringTest {

    @Test
    @DisplayName("application-dev.yml 의 리스너 설정이 팩토리에 동시 소비자 48 · 최대 64 · prefetch 1 로 닿는다")
    void realFactory_devYml_동시소비자48_최대64_prefetch1() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RabbitAutoConfiguration.class))
                .withUserConfiguration(RabbitMQConfig.class, RabbitListenerRetryPolicyCustomizer.class)
                .withInitializer(context -> context.getEnvironment().getPropertySources()
                        .addFirst(devYaml()))
                .run(context -> {
                    SimpleRabbitListenerContainerFactory factory =
                            context.getBean("rabbitListenerContainerFactory", SimpleRabbitListenerContainerFactory.class);

                    assertEquals(48, ReflectionTestUtils.getField(factory, "concurrentConsumers"));
                    assertEquals(64, ReflectionTestUtils.getField(factory, "maxConcurrentConsumers"));
                    assertEquals(1, ReflectionTestUtils.getField(factory, "prefetchCount"));
                });
    }

    private PropertySource<?> devYaml() {
        try {
            return new YamlPropertySourceLoader()
                    .load("application-dev", new ClassPathResource("application-dev.yml")).get(0);
        } catch (IOException e) {
            throw new IllegalStateException("application-dev.yml 을 읽지 못함", e);
        }
    }
}
