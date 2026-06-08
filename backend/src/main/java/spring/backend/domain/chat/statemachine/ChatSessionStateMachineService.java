package spring.backend.domain.chat.statemachine;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

// 세션별 StateMachine 인스턴스를 관리 클래스
@Slf4j
@Service
// @RequiredArgsConstructor -> final 필드 생성자 자동 생성, 의존성 주입을 불변으로 받아 안전하게 사용
@RequiredArgsConstructor
public class ChatSessionStateMachineService {

    // Factory 주입 -> 세션마다 새 머신을 찍어내기 위함, 단일 머신 공유 시 세션 간 상태 오염
    private final StateMachineFactory<ChatSessionState, ChatSessionEvent> stateMachineFactory;
    private final MeterRegistry meterRegistry;
    // 동시 SSE 연결이 같은 Map 을 건드리므로 ConcurrentHashMap 으로 락 없이 스레드 안전 보장
    private final ConcurrentHashMap<Long, StateMachine<ChatSessionState, ChatSessionEvent>> machines = new ConcurrentHashMap<>();

    // 세션의 StateMachine을 생성 및 반환
    // computeIfAbsent -> 같은 세션 동시 요청에도 머신이 단 한 번만 생성되도록 원자적 처리
    public StateMachine<ChatSessionState, ChatSessionEvent> getOrCreateMachine(Long sessionId) {
        return machines.computeIfAbsent(sessionId, id -> {
            StateMachine<ChatSessionState, ChatSessionEvent> machine =
                    stateMachineFactory.getStateMachine(id.toString());
            machine.startReactively().block();
            log.debug("[StateMachine] 생성 - sessionId: {}", id);
            return machine;
        });
    }

    // 이벤트를 전송을 통한 상태 전이
    // boolean 반환 -> 거부된 전이를 호출부가 감지해 잘못된 요청을 막을 수 있게 함
    public boolean sendEvent(Long sessionId, ChatSessionEvent event) {
        StateMachine<ChatSessionState, ChatSessionEvent> machine = machines.get(sessionId);
        // 머신 없음 = 세션 만료/미연결 -> 조용히 무시 대신 예외로 빠르게 실패 처리
        if (machine == null) {
            log.warn("[StateMachine] 존재하지 않는 세션에 이벤트 전송 시도 - sessionId: {}, event: {}",
                    sessionId, event);
            throw new BusinessException(ErrorCode.SESSION_EXPIRED);
        }

        ChatSessionState beforeState = machine.getState().getId();

        Message<ChatSessionEvent> message = MessageBuilder
                .withPayload(event)
                .build();

        boolean accepted = machine.sendEvent(Mono.just(message))
                .blockLast()
                .getResultType() == org.springframework.statemachine.StateMachineEventResult.ResultType.ACCEPTED;

        // 전이 결과 계측 (accepted=정상 전이 / rejected=비정상 전이 차단) -> 대시보드 State Machine 패널
        meterRegistry.counter("chat.statemachine.transition",
                "result", accepted ? "accepted" : "rejected").increment();

        if (accepted) {
            log.debug("[StateMachine] 이벤트 수락 - sessionId: {}, event: {}, {} → {}",
                    sessionId, event, beforeState, machine.getState().getId());
        } else {
            log.warn("[StateMachine] 이벤트 거부 - sessionId: {}, event: {}, 현재 상태: {}",
                    sessionId, event, beforeState);
        }

        return accepted;
    }

    // 현재 상태를 조회
    public ChatSessionState getCurrentState(Long sessionId) {
        StateMachine<ChatSessionState, ChatSessionEvent> machine = machines.get(sessionId);
        if (machine == null) {
            return ChatSessionState.IDLE;
        }
        return machine.getState().getId();
    }

    // 세션 종료 시 StateMachine을 제거
    // 명시적 제거 -> Map 에 머신이 무한 누적되는 메모리 누수 방지
    public void removeMachine(Long sessionId) {
        StateMachine<ChatSessionState, ChatSessionEvent> machine = machines.remove(sessionId);
        if (machine != null) {
            // stop 까지 호출 -> 내부 리액티브 구독/스레드 자원 정리
            machine.stopReactively().block();
            log.debug("[StateMachine] 제거 - sessionId: {}", sessionId);
        }
    }
}
