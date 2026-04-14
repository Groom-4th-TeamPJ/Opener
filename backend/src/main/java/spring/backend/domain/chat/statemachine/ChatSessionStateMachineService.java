package spring.backend.domain.chat.statemachine;

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
@RequiredArgsConstructor
public class ChatSessionStateMachineService {

    private final StateMachineFactory<ChatSessionState, ChatSessionEvent> stateMachineFactory;
    private final ConcurrentHashMap<Long, StateMachine<ChatSessionState, ChatSessionEvent>> machines = new ConcurrentHashMap<>();

    // 세션의 StateMachine을 생성 및 반환
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
    public boolean sendEvent(Long sessionId, ChatSessionEvent event) {
        StateMachine<ChatSessionState, ChatSessionEvent> machine = machines.get(sessionId);
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
    public void removeMachine(Long sessionId) {
        StateMachine<ChatSessionState, ChatSessionEvent> machine = machines.remove(sessionId);
        if (machine != null) {
            machine.stopReactively().block();
            log.debug("[StateMachine] 제거 - sessionId: {}", sessionId);
        }
    }
}
