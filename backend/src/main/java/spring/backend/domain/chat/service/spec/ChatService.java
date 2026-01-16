package spring.backend.domain.chat.service.spec;

import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.backend.domain.chat.dto.request.ChatSaveRequest;
import spring.backend.domain.chat.dto.request.ChatSendRequest;
import spring.backend.domain.chat.dto.request.OpenerAnalysisRequest;

public interface ChatService {

    // 세션 연결 (SSE)
    SseEmitter connectSession(Long sessionId, UUID userId);

    // 메시지 처리 (비동기) 1. 사용자 메시지 Redis 저장 2. LLM API 호출 (스트리밍) 3. SSE로 청크 전송 4. 완료된 응답 Redis 저장
    void processMessageAsync(ChatSendRequest req, UUID userId);

    // 대화 저장 (Redis → PostgreSQL)
    void saveMessagesAsync(ChatSaveRequest req, UUID userId);

    // 명시적 세션 해제
    void disconnectSession(Long sessionId, UUID userId);

    // 오프너 분석 (문제 기반 유사 문제 생성)
    void openerAnalysis(OpenerAnalysisRequest req, UUID userId);
}
