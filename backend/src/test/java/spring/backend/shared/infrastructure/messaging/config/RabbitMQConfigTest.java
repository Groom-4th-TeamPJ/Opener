package spring.backend.shared.infrastructure.messaging.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.amqp.autoconfigure.RabbitProperties;
import org.springframework.boot.amqp.autoconfigure.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

// 직접 만든 rabbitListenerContainerFactory 는 Boot 자동 구성을 대체한다
// 설정자(configurer)를 거치지 않으면 spring.rabbitmq.listener.simple.* 의 prefetch 와 retry 가 한 줄도 적용되지 않는다
class RabbitMQConfigTest {

    @Test
    @DisplayName("YAML 의 prefetch 와 retry 설정이 리스너 컨테이너 팩토리에 적용된다")
    void rabbitListenerContainerFactory_리스너설정_팩토리에적용() throws Exception {
        RabbitProperties properties = bindDevRabbitProperties();
        // 첫 시도 1회 + 재시도 2회 = 총 3회 -> 키 이름이 틀리면 바인딩되지 않고 기본값 3(총 4회)이 남는다
        assertEquals(2, properties.getListener().getSimple().getRetry().getMaxRetries());
        SimpleRabbitListenerContainerFactoryConfigurer configurer =
                new SimpleRabbitListenerContainerFactoryConfigurer(properties);

        SimpleRabbitListenerContainerFactory factory = new RabbitMQConfig()
                .rabbitListenerContainerFactory(mock(ConnectionFactory.class), configurer);

        assertEquals(1, ReflectionTestUtils.getField(factory, "prefetchCount"));
        assertNotNull(factory.getAdviceChain(), "retry 설정이 켜졌는데 재시도 어드바이스가 없음");
        assertTrue(factory.getAdviceChain().length > 0);
        // 재시도 소진 뒤 큐에 되돌리지 않고 DLQ 로 보내는 기존 정책은 유지된다
        assertEquals(false, ReflectionTestUtils.getField(factory, "defaultRequeueRejected"));
    }

    // application-dev.yml 을 실제 바인더로 읽는다 -> 존재하지 않는 키는 오류 없이 무시되므로 값으로만 검증할 수 있다
    private static RabbitProperties bindDevRabbitProperties() throws Exception {
        PropertySource<?> yaml = new YamlPropertySourceLoader()
                .load("dev", new ClassPathResource("application-dev.yml")).get(0);
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(yaml);
        return new Binder(ConfigurationPropertySources.get(environment),
                new PropertySourcesPlaceholdersResolver(environment))
                .bind("spring.rabbitmq", RabbitProperties.class)
                .orElseThrow(() -> new AssertionError("spring.rabbitmq 설정이 바인딩되지 않음"));
    }
}
