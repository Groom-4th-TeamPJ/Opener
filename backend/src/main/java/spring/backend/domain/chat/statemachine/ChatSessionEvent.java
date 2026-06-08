package spring.backend.domain.chat.statemachine;

// 상태를 바꾸는 트리거를 enum 으로 분리 -> 상태 전이 규칙을 이벤트 기준으로 한 곳에서 선언 가능
public enum ChatSessionEvent {
    CONNECT_SUCCESS,   // SSE 연결 성공
    CONNECT_FAIL,      // SSE 연결 실패
    SEND_MESSAGE,      // 사용자 메시지 전송 (LLM/RAG 호출 시작)
    STREAM_START,      // 첫 번째 청크 수신
    STREAM_COMPLETE,   // 스트리밍 완료
    STREAM_ERROR,      // 스트리밍 중 오류 발생
    CLOSE              // 세션 종료 (명시적 disconnect 또는 타임아웃)
}
