package spring.backend.domain.chat.statemachine;

public enum ChatSessionEvent {
    CONNECT_SUCCESS,   // SSE 연결 성공
    CONNECT_FAIL,      // SSE 연결 실패
    SEND_MESSAGE,      // 사용자 메시지 전송 (LLM/RAG 호출 시작)
    STREAM_START,      // 첫 번째 청크 수신
    STREAM_COMPLETE,   // 스트리밍 완료
    STREAM_ERROR,      // 스트리밍 중 오류 발생
    CLOSE              // 세션 종료 (명시적 disconnect 또는 타임아웃)
}
