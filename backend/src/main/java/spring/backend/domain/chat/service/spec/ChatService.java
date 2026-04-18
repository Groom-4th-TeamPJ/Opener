package spring.backend.domain.chat.service.spec;

import java.util.UUID;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import spring.backend.domain.chat.dto.request.ChatSaveRequest;
import spring.backend.domain.chat.dto.request.ChatSendRequest;
import spring.backend.domain.chat.dto.request.OpenerAnalysisRequest;
import spring.backend.domain.chat.dto.response.ChatHistoryResponse;

public interface ChatService {

    // 세션 연결 (SSE) — Flux 기반 non-blocking 스트리밍
    Flux<ServerSentEvent<String>> connectSession(Long sessionId, UUID userId);

    // 메시지 처리 (non-blocking subscribe)
    void processMessage(ChatSendRequest req, UUID userId);

    // 대화 저장 (Redis → PostgreSQL via RabbitMQ)
    void saveMessages(ChatSaveRequest req, UUID userId);

    // 명시적 세션 해제
    void disconnectSession(Long sessionId, UUID userId);

    // 오프너 분석 (문제 기반 유사 문제 생성)
    void openerAnalysis(OpenerAnalysisRequest req, UUID userId);

    // 채팅 히스토리 조회 (내부 호출용)
    ChatHistoryResponse getChatHistoryByQuestionResultId(Long questionResultId);
}
