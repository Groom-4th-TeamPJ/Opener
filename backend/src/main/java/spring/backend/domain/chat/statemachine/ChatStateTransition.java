package spring.backend.domain.chat.statemachine;

// 전이 1건 -> 설정과 테스트가 같은 값을 보게 하기 위한 최소 단위
public record ChatStateTransition(
        ChatSessionState source,
        ChatSessionEvent event,
        ChatSessionState target
) {
}
