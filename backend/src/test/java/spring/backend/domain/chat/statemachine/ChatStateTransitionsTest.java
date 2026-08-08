package spring.backend.domain.chat.statemachine;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 전이 테이블은 이 서비스의 방어선인데 지금까지 목으로만 대체돼 한 번도 실행되지 않았다
class ChatStateTransitionsTest {

    @Test
    @DisplayName("스트리밍 중 재전송은 거부된다")
    void allows_스트리밍중_SEND_MESSAGE_거부된다() {
        // 허용하면 한 세션에 두 스트림이 겹쳐 청크가 뒤섞인다
        assertFalse(ChatStateTransitions.allows(
                ChatSessionState.STREAMING, ChatSessionEvent.SEND_MESSAGE));
    }

    @Test
    @DisplayName("연결 전 메시지 전송은 거부된다")
    void allows_IDLE에서_SEND_MESSAGE_거부된다() {
        // Sink 가 없으므로 응답을 보낼 곳이 없다
        assertFalse(ChatStateTransitions.allows(
                ChatSessionState.IDLE, ChatSessionEvent.SEND_MESSAGE));
    }

    @Test
    @DisplayName("에러 상태에서는 같은 연결로 재전송할 수 있다")
    void allows_에러상태에서_SEND_MESSAGE_허용된다() {
        // 막으면 한 번 실패한 세션이 재연결 없이는 복구되지 않는다
        assertTrue(ChatStateTransitions.allows(
                ChatSessionState.STREAM_ERROR, ChatSessionEvent.SEND_MESSAGE));
        assertTrue(ChatStateTransitions.allows(
                ChatSessionState.PROCESSING_ERROR, ChatSessionEvent.SEND_MESSAGE));
    }

    @Test
    @DisplayName("빈 응답 회수 경로가 열려 있다")
    void allows_PROCESSING에서_STREAM_ERROR_허용된다() {
        // 청크가 하나도 안 온 채 실패하면 STREAMING 을 거치지 않는다
        // 이 전이가 없으면 세션이 PROCESSING 에 갇힌다
        assertTrue(ChatStateTransitions.allows(
                ChatSessionState.PROCESSING, ChatSessionEvent.STREAM_ERROR));
    }

    @Test
    @DisplayName("모든 상태에 도달하는 전이가 선언돼 있다")
    void ALL_모든상태가_전이대상에포함된다() {
        Set<ChatSessionState> reachable = ChatStateTransitions.ALL.stream()
                .map(ChatStateTransition::target)
                .collect(Collectors.toSet());
        reachable.add(ChatSessionState.IDLE); // 초기 상태

        // enum 에만 추가하고 전이를 안 만들면 도달 불가능한 상태가 생긴다
        assertTrue(reachable.containsAll(EnumSet.allOf(ChatSessionState.class)));
    }

    @Test
    @DisplayName("모든 상태에서 CLOSE 로 IDLE 로 빠져나갈 수 있다")
    void allows_모든상태에서_CLOSE_허용된다() {
        EnumSet.complementOf(EnumSet.of(ChatSessionState.IDLE))
                .forEach(state -> assertTrue(
                        ChatStateTransitions.allows(state, ChatSessionEvent.CLOSE),
                        state + " 에서 CLOSE 가 막혀 있다"));
    }
}
