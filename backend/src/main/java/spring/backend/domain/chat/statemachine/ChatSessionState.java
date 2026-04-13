package spring.backend.domain.chat.statemachine;

public enum ChatSessionState {
    IDLE,        // 초기 상태 (연결 전)
    CONNECTED,   // SSE 연결 완료, 메시지 대기 중
    PROCESSING,  // LLM/RAG 요청 전송됨, 응답 대기 중
    STREAMING,   // 스트리밍 청크 수신 중
    COMPLETED,   // 스트리밍 완료, 새 메시지 대기 중
    ERROR        // 오류 발생
}
