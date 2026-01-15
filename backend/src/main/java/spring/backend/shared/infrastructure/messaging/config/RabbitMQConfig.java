package spring.backend.shared.infrastructure.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue 이름
    public static final String CHAT_MESSAGE_SAVE_QUEUE = "chat.message.save.queue";

    // Exchange 이름
    public static final String CHAT_MESSAGE_EXCHANGE = "chat.message.exchange";

    // Routing Key
    public static final String CHAT_MESSAGE_SAVE_ROUTING_KEY = "chat.message.save";

    // 채팅 메시지 저장 큐 선언 durable=true: 서버 재시작시에도 큐 유지(영속성)
    @Bean
    public Queue chatMessageSaveQueue() {
        return new Queue(CHAT_MESSAGE_SAVE_QUEUE, true);
    }

    // Direct Exchange 선언 Direct Exchange: Routing Key가 정확히 일치하는 큐로만 메시지 전송
    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(CHAT_MESSAGE_EXCHANGE);
    }

    // Queue와 Exchange 바인딩 Routing Key를 사용하여 특정 큐로 메시지 라우팅
    @Bean
    public Binding chatMessageSaveBinding(Queue chatMessageSaveQueue, DirectExchange chatExchange) {
        return BindingBuilder
                .bind(chatMessageSaveQueue)
                .to(chatExchange)
                .with(CHAT_MESSAGE_SAVE_ROUTING_KEY);
    }

    // 메시지 컨버터 - JSON 직렬화/역직렬화 Java 객체 <-> JSON 자동 변환
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    // RabbitTemplate - 메시지 발행용 Producer가 사용할 템플릿
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    // Listener Container Factory - 메시지 수신 설정 Consumer가 사용할 팩토리
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}
