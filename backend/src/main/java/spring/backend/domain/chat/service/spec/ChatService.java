package spring.backend.domain.chat.service.spec;

import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.backend.domain.chat.dto.request.ChatSendRequest;

public interface ChatService {

    // 세션 연결
    SseEmitter connectSession(Long sessionId, UUID userId);

    // 메시지 처리 (비동기) 1. 사용자 메시지 Redis 저장 2. LLM API 호출 (스트리밍) 3. SSE로 청크 전송 4. 완료된 응답 Redis 저장
    void processUserMessageAsync(ChatSendRequest req, UUID userId);

    // 대화 저장 (Redis → PostgreSQL)
    void saveConversationAsync(String sessionId, UUID userId);

    // 명시적 세션 해제
    void disconnectSession(Long sessionId, UUID userId);
}
