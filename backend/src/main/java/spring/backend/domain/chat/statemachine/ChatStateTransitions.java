package spring.backend.domain.chat.statemachine;

import java.util.List;

// 전이 선언의 단일 소스
//
// Spring StateMachine 의 EnumStateMachineConfigurerAdapter 는 @SpringBootTest 없이 실행할 수 없어,
// 설정에 전이를 직접 적으면 전이 테이블을 단위 테스트할 방법이 없다.
// 순수 데이터로 분리해 설정과 테스트가 같은 값을 보게 한다
public final class ChatStateTransitions {

    private ChatStateTransitions() {
    }

    public static final List<ChatStateTransition> ALL = List.of(
            // 연결
            t(ChatSessionState.IDLE, ChatSessionEvent.CONNECT_SUCCESS, ChatSessionState.CONNECTED),
            t(ChatSessionState.IDLE, ChatSessionEvent.CONNECT_FAIL, ChatSessionState.CONNECTION_ERROR),

            // 정상 흐름
            t(ChatSessionState.CONNECTED, ChatSessionEvent.SEND_MESSAGE, ChatSessionState.PROCESSING),
            t(ChatSessionState.PROCESSING, ChatSessionEvent.STREAM_START, ChatSessionState.STREAMING),
            t(ChatSessionState.STREAMING, ChatSessionEvent.STREAM_COMPLETE, ChatSessionState.COMPLETED),

            // 오류 회수 - PROCESSING 에서의 STREAM_ERROR 는 청크가 하나도 안 온 빈 응답 경로다
            t(ChatSessionState.PROCESSING, ChatSessionEvent.STREAM_ERROR, ChatSessionState.PROCESSING_ERROR),
            t(ChatSessionState.STREAMING, ChatSessionEvent.STREAM_ERROR, ChatSessionState.STREAM_ERROR),

            // 재전송 - 같은 연결을 유지한 채 다음 질문을 받는다
            t(ChatSessionState.COMPLETED, ChatSessionEvent.SEND_MESSAGE, ChatSessionState.PROCESSING),
            t(ChatSessionState.STREAM_ERROR, ChatSessionEvent.SEND_MESSAGE, ChatSessionState.PROCESSING),
            t(ChatSessionState.PROCESSING_ERROR, ChatSessionEvent.SEND_MESSAGE, ChatSessionState.PROCESSING),

            // 종료 - 어느 상태에서든 빠져나갈 수 있어야 세션이 갇히지 않는다
            t(ChatSessionState.CONNECTED, ChatSessionEvent.CLOSE, ChatSessionState.IDLE),
            t(ChatSessionState.COMPLETED, ChatSessionEvent.CLOSE, ChatSessionState.IDLE),
            t(ChatSessionState.PROCESSING, ChatSessionEvent.CLOSE, ChatSessionState.IDLE),
            t(ChatSessionState.STREAMING, ChatSessionEvent.CLOSE, ChatSessionState.IDLE),
            t(ChatSessionState.STREAM_ERROR, ChatSessionEvent.CLOSE, ChatSessionState.IDLE),
            t(ChatSessionState.PROCESSING_ERROR, ChatSessionEvent.CLOSE, ChatSessionState.IDLE),
            t(ChatSessionState.CONNECTION_ERROR, ChatSessionEvent.CLOSE, ChatSessionState.IDLE)
    );

    // 이 (상태, 이벤트) 조합이 선언돼 있는가
    public static boolean allows(ChatSessionState source, ChatSessionEvent event) {
        return ALL.stream()
                .anyMatch(tr -> tr.source() == source && tr.event() == event);
    }

    private static ChatStateTransition t(
            ChatSessionState source, ChatSessionEvent event, ChatSessionState target) {
        return new ChatStateTransition(source, event, target);
    }
}
