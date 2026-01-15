package spring.backend.domain.chat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.backend.domain.chat.dto.request.ChatSaveRequest;
import spring.backend.domain.chat.dto.request.ChatSendRequest;
import spring.backend.domain.chat.service.spec.ChatService;
import spring.backend.shared.infrastructure.security.dto.AuthUser;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // sse 연결
    @GetMapping(value = "/connect/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal AuthUser authUser) {

        return chatService.connectSession(sessionId, authUser.id());
    }

    // 메시지 전송 (사용자 → 서버) POST /api/chat/message
    @PostMapping("/message")
    public ResponseEntity<Void> sendMessage(
            @RequestBody ChatSendRequest req,
            @AuthenticationPrincipal AuthUser authUser) {

        chatService.processMessageAsync(req, authUser.id());

        return ResponseEntity.ok().build();
    }

    // 대화 저장 (Redis → PostgreSQL) POST /api/chat/conversations/{sessionId}/save
    @PostMapping("/save-message")
    public ResponseEntity<Void> saveConversation(
            @RequestBody ChatSaveRequest req,
            @AuthenticationPrincipal AuthUser authUser) {

        chatService.saveMessagesAsync(req, authUser.id());

        return ResponseEntity.ok().build();
    }

    // 명시적 세션 해제
    @PostMapping("/disconnect/{sessionId}")
    public ResponseEntity<Void> disconnectSession(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal AuthUser authUser) {
        chatService.disconnectSession(sessionId, authUser.id());

        return null;
    }

    // 오프너 분석
    @PostMapping("/opener-analysis/{questionId}")
    public ResponseEntity<Void> openerAnalysis(
            @RequestBody OpenerAnalysisRequest req
    ) {
        chatService.openerAnalysis(req);

        return null;
    }
}
