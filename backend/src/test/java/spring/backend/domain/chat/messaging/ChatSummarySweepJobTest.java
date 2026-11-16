package spring.backend.domain.chat.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;

// 요약 이벤트 발행이 실패하거나 요약 큐가 DLQ 로 간 행은 summary 가 null 로 남는다 -> 주기 점검이 그 행의 이벤트를 다시 낸다
@ExtendWith(MockitoExtension.class)
class ChatSummarySweepJobTest {

    private static final long MIN_AGE_MS = 300_000L;
    private static final int BATCH_SIZE = 500;

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ChatMessageProducer chatMessageProducer;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private ChatSummarySweepJob job() {
        return new ChatSummarySweepJob(chatMessageRepository, chatMessageProducer, meterRegistry, MIN_AGE_MS, BATCH_SIZE);
    }

    private double count(String name) {
        return meterRegistry.counter(name).count();
    }

    @Test
    @DisplayName("한 번의 점검은 현재 시각에서 최소 나이를 뺀 기준과 배치 크기를 저장소에 넘기고 id 순서대로 이벤트를 낸다")
    void sweep_요약없는행_기준시각과한도를넘기고_id순서대로발행한다() {
        when(chatMessageRepository.findPendingSummaryIds(any(), anyInt())).thenReturn(List.of(3L, 5L, 9L));

        LocalDateTime before = LocalDateTime.now().minus(java.time.Duration.ofMillis(MIN_AGE_MS));
        job().sweep();
        LocalDateTime after = LocalDateTime.now().minus(java.time.Duration.ofMillis(MIN_AGE_MS));

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(chatMessageRepository).findPendingSummaryIds(cutoff.capture(), org.mockito.ArgumentMatchers.eq(BATCH_SIZE));
        // 기준이 now + 최소 나이면 방금 저장돼 큐에 있는 행까지 다시 발행한다 -> 기준은 과거 쪽이어야 한다
        assertFalse(cutoff.getValue().isBefore(before));
        assertFalse(cutoff.getValue().isAfter(after));

        InOrder order = inOrder(chatMessageProducer);
        order.verify(chatMessageProducer).publishSummaryEvent(3L);
        order.verify(chatMessageProducer).publishSummaryEvent(5L);
        order.verify(chatMessageProducer).publishSummaryEvent(9L);
        assertEquals(3.0, count("chat.summary.sweep.republished"));
        assertEquals(0.0, count("chat.summary.sweep.publish_failed"));
    }

    @Test
    @DisplayName("발행이 한 건 실패해도 세고 다음 id 로 계속 간다")
    void sweep_발행한건실패_실패로세고나머지를계속발행한다() {
        when(chatMessageRepository.findPendingSummaryIds(any(), anyInt())).thenReturn(List.of(3L, 5L, 9L));
        // 엄격 스텁은 5L 이 아닌 호출(3L, 9L)을 인자 불일치로 막는다 -> 5L 만 던지게 느슨하게 건다
        lenient().doThrow(new AmqpException("broker down")).when(chatMessageProducer).publishSummaryEvent(5L);

        job().sweep();

        verify(chatMessageProducer).publishSummaryEvent(3L);
        verify(chatMessageProducer).publishSummaryEvent(5L);
        // 첫 실패에서 멈추면 뒤 행이 다음 점검까지 요약 없이 남는다
        verify(chatMessageProducer).publishSummaryEvent(9L);
        assertEquals(2.0, count("chat.summary.sweep.republished"));
        assertEquals(1.0, count("chat.summary.sweep.publish_failed"));
    }

    @Test
    @DisplayName("요약 없는 행이 없으면 발행하지 않고 셈도 늘지 않는다")
    void sweep_대상없음_아무것도발행하지않는다() {
        when(chatMessageRepository.findPendingSummaryIds(any(), anyInt())).thenReturn(List.of());

        job().sweep();

        verifyNoInteractions(chatMessageProducer);
        assertEquals(0.0, count("chat.summary.sweep.republished"));
        assertEquals(0.0, count("chat.summary.sweep.publish_failed"));
    }

    @Test
    @DisplayName("저장소 조회가 실패하면 예외를 밖으로 던지지 않는다")
    void sweep_저장소조회실패_예외없이끝나고다음주기를기다린다() {
        when(chatMessageRepository.findPendingSummaryIds(any(), anyInt())).thenThrow(new IllegalStateException("db down"));

        // 스케줄러 스레드로 예외가 올라가도 다음 주기는 돈다 -> 그래도 로그 없이 사라지지 않게 잡아서 기록한다
        job().sweep();

        verify(chatMessageProducer, never()).publishSummaryEvent(any());
        assertTrue(count("chat.summary.sweep.republished") == 0.0);
    }
}
