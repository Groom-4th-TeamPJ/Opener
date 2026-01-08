package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.backend.domain.chat.dto.ChatMessageDto;
import spring.backend.domain.chat.dto.SseMessageDto;
import spring.backend.domain.chat.service.spec.ChatRedisService;
import spring.backend.domain.chat.service.spec.ChatService;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.domain.user.model.entity.User;
import spring.backend.domain.user.repository.spec.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

  // SSE 타임아웃 (5분)
  private static final Long SSE_TIMEOUT = 5 * 60 * 1000L;
  private final ChatRedisService chatRedisService;
  private final LlmService llmService;
  private final ObjectMapper objectMapper;
  private final UserRepository userRepository;

  // SSE 연결 관리 (sessionId → SseEmitter)
  private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

  // 세션 관리 (userId, sessionId)
  private final ConcurrentHashMap<UUID, Long> sessions = new ConcurrentHashMap<>();

  @Override
  @Transactional
  public SseEmitter connectSession(Long sessionId, UUID userId) {
    // 스프링 인메모리 힙에 sessionId로 운영중인 SSE 연결 조회
    SseEmitter sseEmitter = emitters.get(sessionId);

    // 기존 SSE 연결이 있으면 재사용
    if (sseEmitter != null) {

      // DB에서 sessionId와 userId로 세션 권한 검증
      validateSessionOwner(userId, sessionId);

      return sseEmitter;
    }

    // 새 SSE 연결 생성
    try {
      SseEmitter newEmitter = new SseEmitter(SSE_TIMEOUT);

      // 세션 해제 동작
      newEmitter.onCompletion(() -> emitters.remove(sessionId));

      // 타임아웃시 ConcurrentHashMap 에서 emitter 제거
      newEmitter.onTimeout(() -> emitters.remove(sessionId));

      // 세션 예외 발생 처리
      newEmitter.onError(e -> emitters.remove(sessionId));

      // sessionId로 Emitter 저장
      emitters.put(sessionId, newEmitter);

      // userId로 sessionId 저장
      sessions.put(userId, sessionId);

      User user = userRepository.findUserById(userId);

      // 세션용 채팅 이력 관리 레디스 초기화
      chatRedisService.initializeSession(sessionId, userId);

      return newEmitter;

    } catch (Exception e) {
      // 오류시 새롭게 생성된 세션 삭제
      sessions.remove(userId);
      emitters.remove(sessionId);
      chatRedisService.deleteSession(sessionId);

      throw new RuntimeException("연결 생성에 실패하였습니다.", e);
    }
  }

  // 세션 주인 확인 (권한 없으면 예외 throw)
  @Override
  public void validateSessionOwner(UUID userId, Long sessionId) {

    sessions.get(userId);

    if (sessionId != sessions.get(userId)) {
      throw new IllegalArgumentException(
              "세션 접근 권한이 없습니다.: sessionId=" + sessionId + ", userId=" + userId);
    }
  }

  @Async
  @Override
  public void processMessageAsync(String sessionId, String content, SseEmitter emitter) {
    try {
      // 1. 사용자 메시지 저장 (Redis)
      ChatMessageDto userMessage =
              ChatMessageDto.builder()
                      .sessionId(sessionId)
                      .role("USER")
                      .content(content)
                      .timestamp(LocalDateTime.now())
                      .build();
      chatRedisService.saveMessage(sessionId, userMessage);

      // 2. LLM API 호출 (스트리밍)
      StringBuilder fullResponse = new StringBuilder();

      llmService.chatStream(
              sessionId,
              content,
              chunk -> {
                // 각 청크마다 SSE 전송
                fullResponse.append(chunk);
                sendSseChunk(emitter, sessionId, chunk);
              });

      // 3. 완료된 응답 저장 (Redis)
      ChatMessageDto assistantMessage =
              ChatMessageDto.builder()
                      .sessionId(sessionId)
                      .role("ASSISTANT")
                      .content(fullResponse.toString())
                      .timestamp(LocalDateTime.now())
                      .tokenUsage(150) // TODO: 실제 토큰 수 계산
                      .build();
      chatRedisService.saveMessage(sessionId, assistantMessage);

      // 4. 완료 이벤트 전송
      sendSseComplete(emitter, sessionId, 150);

    } catch (Exception e) {
      log.error("Error processing message: {}", e.getMessage(), e);
      sendSseError(emitter, sessionId, e.getMessage());
    }
  }

  @Async
  @Transactional
  @Override
  public void saveConversationAsync(String sessionId, UUID userId) {
    // TODO: Redis → PostgreSQL 마이그레이션
    log.info("Saving conversation: sessionId={}, userId={}", sessionId, userId);
  }

  /**
   * SSE 청크 전송
   */
  private void sendSseChunk(SseEmitter emitter, String sessionId, String chunk) {
    try {
      SseMessageDto message =
              SseMessageDto.builder().type("chunk").sessionId(sessionId).chunk(chunk).build();

      emitter.send(
              SseEmitter.event().name("message").data(objectMapper.writeValueAsString(message)));

    } catch (Exception e) {
      log.error("Failed to send SSE chunk: {}", e.getMessage());
    }
  }

  /**
   * SSE 완료 이벤트 전송
   */
  private void sendSseComplete(SseEmitter emitter, String sessionId, int totalTokens) {
    try {
      SseMessageDto message =
              SseMessageDto.builder()
                      .type("complete")
                      .sessionId(sessionId)
                      .totalTokens(totalTokens)
                      .build();

      emitter.send(
              SseEmitter.event().name("complete").data(objectMapper.writeValueAsString(message)));

      emitter.complete(); // SSE 연결 종료

    } catch (Exception e) {
      log.error("Failed to send SSE complete: {}", e.getMessage());
    }
  }

  /**
   * SSE 에러 전송
   */
  private void sendSseError(SseEmitter emitter, String sessionId, String error) {
    try {
      SseMessageDto message =
              SseMessageDto.builder().type("error").sessionId(sessionId).error(error).build();

      emitter.send(
              SseEmitter.event().name("error").data(objectMapper.writeValueAsString(message)));

      emitter.completeWithError(new RuntimeException(error));

    } catch (Exception e) {
      log.error("Failed to send SSE error: {}", e.getMessage());
    }
  }
}
