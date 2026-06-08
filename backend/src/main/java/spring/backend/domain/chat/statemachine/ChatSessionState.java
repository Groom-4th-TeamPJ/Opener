package spring.backend.domain.chat.statemachine;

// 채팅 세션의 가능한 상태를 enum 으로 고정 -> 문자열/boolean 플래그 난립 막고 잘못된 전이를 컴파일 단위로 차단
public enum ChatSessionState {
    IDLE,        // 초기 상태 (연결 전)
    CONNECTED,   // SSE 연결 완료, 메시지 대기 중
    PROCESSING,  // LLM/RAG 요청 전송됨, 응답 대기 중
    STREAMING,   // 스트리밍 청크 수신 중
    COMPLETED,   // 스트리밍 완료, 새 메시지 대기 중
    STREAM_ERROR,    // 스트리밍 오류 발생
    CONNECTION_ERROR,     // 연결 오류 발생
    PROCESSING_ERROR    // 요청 오류
}
