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

// 채팅 영속화를 비동기 큐로 분리 -> DB 저장 지연이 사용자 응답(SSE)을 막지 않게 함
@Configuration
public class RabbitMQConfig {

    // 프로필 주입 -> dev 에서만 리스너 수동 시작하기 위해 런타임 환경 판별
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

    // durable=true -> 브로커 재시작에도 큐와 메시지 유지, 저장 이벤트 유실 방지
    // DLX 연결 -> 처리 실패 메시지를 버리지 않고 DLQ 로 보내 추후 재처리/분석 가능
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

    // Direct Exchange -> 라우팅 키 정확 일치 큐로만 전송, 단순 1:1 라우팅에 가장 가볍고 명확
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

    // JSON 컨버터 -> 언어/버전 독립 포맷으로 전송, 자바 기본 직렬화 의존성 문제 회피
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

        // requeue=false -> 실패 메시지를 같은 큐에 재투입하지 않음, 무한 재시도 루프 차단하고 DLQ 로 보냄
        factory.setDefaultRequeueRejected(false);

        // dev 는 자동 시작 끔 -> DevQueueCleaner 가 옛 메시지 비운 뒤 수동 시작해야 깨끗한 상태 보장
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
