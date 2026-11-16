package spring.backend.domain.chat.messaging;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;

// 요약 이벤트 발행이 실패했거나 요약 큐가 DLQ 로 보낸 행은 summary 가 null 로 남는다 -> 오래된 것부터 이벤트를 다시 낸다
// 요약 소비자는 summary 가 채워진 행을 건너뛰므로 같은 행을 두 번 발행해도 결과는 같다
@Slf4j
@Component
public class ChatSummarySweepJob {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageProducer chatMessageProducer;
    private final MeterRegistry meterRegistry;
    private final long minAgeMs;
    private final int batchSize;

    public ChatSummarySweepJob(
            ChatMessageRepository chatMessageRepository,
            ChatMessageProducer chatMessageProducer,
            MeterRegistry meterRegistry,
            @Value("${chat.summary.sweep.min-age-ms:300000}") long minAgeMs,
            @Value("${chat.summary.sweep.batch-size:500}") int batchSize
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatMessageProducer = chatMessageProducer;
        this.meterRegistry = meterRegistry;
        this.minAgeMs = minAgeMs;
        this.batchSize = batchSize;
    }

    // fixedDelay -> 앞 점검이 끝난 뒤부터 간격을 센다, 점검이 길어져도 겹쳐 돌지 않는다
    // shortcut: 첫 요약 이벤트가 아직 큐에 있거나 처리 중인 행도 최소 나이(5분)가 지나면 다시 발행해 LLM 을 한 번 더 부른다(소비자는 요약이 저장된 뒤에야 건너뜀), 큐 적체가 최소 나이를 넘기면 이 중복이 는다
    // shortcut: 요약이 매번 실패해 DLQ 로 가는 행은 오래된 id 순 조회에서 매 점검마다 맨 앞에 돌아온다, 그런 행이 배치 크기보다 많아지면 더 새 행에는 닿지 않는다 -> 점검을 거듭해도 미요약 행 수가 0 위로 남으면 시도 횟수나 마지막 시도 시각 컬럼을 두고 조회가 건너뛰게 한다
    // shortcut: 인스턴스가 여럿이면 같은 행을 각자 발행한다, 다중 인스턴스로 배포할 때 점검 잠금 추가
    @Scheduled(fixedDelayString = "${chat.summary.sweep.interval-ms:60000}")
    public void sweep() {
        List<Long> ids;
        try {
            LocalDateTime cutoff = LocalDateTime.now().minus(Duration.ofMillis(minAgeMs));
            ids = chatMessageRepository.findPendingSummaryIds(cutoff, batchSize);
        } catch (RuntimeException e) {
            // 조회 실패가 스케줄러 스레드의 예외로 끝나면 원인이 로그에 한 줄 남지 않는다 -> 잡아서 기록하고 다음 주기를 기다린다
            log.error("[Summary Sweep] 미요약 행 조회 실패", e);
            return;
        }

        int republished = 0;
        int failed = 0;
        for (Long id : ids) {
            try {
                chatMessageProducer.publishSummaryEvent(id);
                republished++;
            } catch (RuntimeException e) {
                // 한 건 실패로 멈추면 뒤 행이 다음 주기까지 밀린다 -> 세고 다음 id 로 간다
                failed++;
                log.warn("[Summary Sweep] 요약 이벤트 재발행 실패 - chatMessageId: {}", id, e);
            }
        }

        meterRegistry.counter("chat.summary.sweep.republished").increment(republished);
        meterRegistry.counter("chat.summary.sweep.publish_failed").increment(failed);
        if (!ids.isEmpty()) {
            log.info("[Summary Sweep] 점검 끝 - 대상: {}, 재발행: {}, 실패: {}", ids.size(), republished, failed);
        }
    }
}
