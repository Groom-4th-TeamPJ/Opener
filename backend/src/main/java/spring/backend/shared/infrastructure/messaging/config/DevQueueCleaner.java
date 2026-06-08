package spring.backend.shared.infrastructure.messaging.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 개발 환경에서 애플리케이션 시작 시 RabbitMQ 큐를 자동으로 정리하는 컴포넌트
 *
 * 개발 중 오래된 테스트 메시지가 큐에 남아있어 서비스 시작 시 오류를 발생시키는 것을 방지합니다.
 * 큐 정리 후 RabbitListener를 수동으로 시작하여 정리된 상태에서 메시지 처리를 시작합니다.
 */
@Slf4j
@Component
// @Profile("dev") -> 개발 환경에서만 빈 등록, 운영 데이터가 삭제되는 사고 원천 차단
@Profile("dev")
@RequiredArgsConstructor
public class DevQueueCleaner {

    private final RabbitAdmin rabbitAdmin;
    private final RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    // ApplicationReadyEvent 시점 -> 빈 초기화 완전히 끝난 뒤 실행, 큐 정리 중 메시지 소비 충돌 방지
    @EventListener(ApplicationReadyEvent.class)
    public void clearQueuesOnStartup() {
        log.info("=== [DEV] 개발 환경 감지: RabbitMQ 큐 정리 시작 ===");

        try {
            // 1. 채팅 메시지 저장 큐 정리 (RabbitListener는 아직 시작 안 됨)
            purgeQueue(RabbitMQConfig.CHAT_MESSAGE_SAVE_QUEUE);

            log.info("=== [DEV] RabbitMQ 큐 정리 완료 ===");

            // 2. 큐 정리가 완료되었으므로 이제 RabbitListener 시작
            rabbitListenerEndpointRegistry.getListenerContainers().forEach(container -> {
                if (!container.isRunning()) {
                    log.info("[DEV] RabbitListener 컨테이너 시작 중...");
                    container.start();
                }
            });

            log.info("=== [DEV] RabbitListener 시작 완료 - 서비스 준비 완료 ===");

        } catch (Exception e) {
            log.error("=== [DEV] RabbitMQ 큐 정리 또는 리스너 시작 중 오류 발생 ===", e);
        }
    }

    private void purgeQueue(String queueName) {
        Integer messageCount = rabbitAdmin.purgeQueue(queueName);
        if (messageCount != null && messageCount > 0) {
            log.info("[DEV] 큐 '{}' 에서 {} 개의 메시지 제거됨", queueName, messageCount);
        } else {
            log.info("[DEV] 큐 '{}' 는 비어있거나 존재하지 않음", queueName);
        }
    }
}
