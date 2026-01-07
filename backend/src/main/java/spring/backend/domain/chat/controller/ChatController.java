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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.backend.domain.chat.dto.request.ChatSendRequest;
import spring.backend.domain.chat.dto.response.ChatSessionResponse;
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

  // 2. 세션 초기화 (새로운 대화 시작) POST /api/chat/sessions
  @PostMapping("/sessions")
  public ResponseEntity<ChatSessionResponse> createSession(
          @RequestParam(required = false) Long questionId,
          @AuthenticationPrincipal AuthUser authUser) {

    String sessionId = chatService.createSession(authUser.id(), questionId);

    return ResponseEntity.ok(ChatSessionResponse.builder().sessionId(sessionId).build());
  }

  /**
   * 3. 메시지 전송 (사용자 → 서버) POST /api/chat/send
   */
  @PostMapping("/send")
  public ResponseEntity<Void> sendMessage(
          @RequestBody ChatSendRequest request, @AuthenticationPrincipal AuthUser authUser) {

    // 세션 소유권 검증
    chatService.validateSessionOwner(request.getSessionId(), authUser.id());

    // SSE Emitter 조회
    SseEmitter emitter = emitters.get(request.getSessionId());
    if (emitter == null) {
      return ResponseEntity.badRequest().build();
    }

    // 비동기 처리 (메시지 저장 + LLM 호출 + SSE 스트리밍)
    chatService.processMessageAsync(request.getSessionId(), request.getContent(), emitter);

    return ResponseEntity.accepted().build(); // 202 Accepted
  }

  /**
   * 4. 대화 저장 (Redis → PostgreSQL) POST /api/chat/conversations/{sessionId}/save
   */
  @PostMapping("/conversations/{sessionId}/save")
  public ResponseEntity<Void> saveConversation(
          @PathVariable String sessionId, @AuthenticationPrincipal AuthUser authUser) {

    chatService.saveConversationAsync(sessionId, authUser.id());

    return ResponseEntity.accepted().build(); // 202 Accepted
  }

  /**
   * 5. 저장된 대화 목록 조회 GET /api/chat/conversations
   */
  @GetMapping("/conversations")
  public ResponseEntity<?> getConversations(@AuthenticationPrincipal AuthUser authUser) {
    // TODO: 구현
    log.info("Get conversations request: userId={}", authUser.id());
    return ResponseEntity.ok().build();
  }
}
