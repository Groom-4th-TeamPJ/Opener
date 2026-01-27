package spring.backend.shared.infrastructure.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    // Queue 이름
    public static final String CHAT_MESSAGE_SAVE_QUEUE = "chat.message.save.queue";
    public static final String CHAT_MESSAGE_DLQ = "chat.message.save.dlq"; // Dead Letter Queue

    // Exchange 이름
    public static final String CHAT_MESSAGE_EXCHANGE = "chat.message.exchange";
    public static final String CHAT_MESSAGE_DLX = "chat.message.dlx"; // Dead Letter Exchange

    // Routing Key
    public static final String CHAT_MESSAGE_SAVE_ROUTING_KEY = "chat.message.save";
    public static final String CHAT_MESSAGE_DLQ_ROUTING_KEY = "chat.message.save.dlq";

    // 채팅 메시지 저장 큐 선언 durable=true: 서버 재시작시에도 큐 유지(영속성)
    // DLX(Dead Letter Exchange) 설정: 메시지 처리 실패 시 DLQ로 라우팅
    @Bean
    public Queue chatMessageSaveQueue() {
        return org.springframework.amqp.core.QueueBuilder
                .durable(CHAT_MESSAGE_SAVE_QUEUE)
                .withArgument("x-dead-letter-exchange", CHAT_MESSAGE_DLX)
                .withArgument("x-dead-letter-routing-key", CHAT_MESSAGE_DLQ_ROUTING_KEY)
                .build();
    }

    // Dead Letter Queue - 실패한 메시지 저장
    @Bean
    public Queue chatMessageDlq() {
        return new Queue(CHAT_MESSAGE_DLQ, true);
    }

    // Direct Exchange 선언 Direct Exchange: Routing Key가 정확히 일치하는 큐로만 메시지 전송
    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(CHAT_MESSAGE_EXCHANGE);
    }

    // Dead Letter Exchange
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(CHAT_MESSAGE_DLX);
    }

    // Queue와 Exchange 바인딩 Routing Key를 사용하여 특정 큐로 메시지 라우팅
    @Bean
    public Binding chatMessageSaveBinding(Queue chatMessageSaveQueue, DirectExchange chatExchange) {
        return BindingBuilder
                .bind(chatMessageSaveQueue)
                .to(chatExchange)
                .with(CHAT_MESSAGE_SAVE_ROUTING_KEY);
    }

    // DLQ와 DLX 바인딩
    @Bean
    public Binding deadLetterBinding(Queue chatMessageDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder
                .bind(chatMessageDlq)
                .to(deadLetterExchange)
                .with(CHAT_MESSAGE_DLQ_ROUTING_KEY);
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

        // 재시도 실패 시 메시지를 DLQ로 보냄 (reject하여 Dead Letter Exchange로 라우팅)
        factory.setDefaultRequeueRejected(false); // reject된 메시지를 다시 큐에 넣지 않음

        // 개발 환경에서는 자동 시작 안 함 (DevQueueCleaner가 큐 정리 후 수동 시작)
        if ("dev".equals(activeProfile)) {
            factory.setAutoStartup(false);
        }

        return factory;
    }

    // RabbitAdmin - 큐 관리 및 운영 작업용
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
