package spring.backend.domain.chat.service.spec;

import java.util.List;
import java.util.UUID;
import spring.backend.domain.chat.dto.response.ChatMessageDto;

public interface ChatRedisService {

  // 세션 초기화
  void initializeSession(Long sessionId, UUID userId);

  // 메시지 저장
  void saveMessage(String sessionId, ChatMessageDto message);

  // 세션의 모든 메시지 조회
  List<ChatMessageDto> getSessionMessages(String sessionId);

  // 세션 소유자 ID 조회
  UUID getSessionOwnerId(String sessionId);

  // 세션 삭제
  void deleteSession(String sessionId);

  // 세션 권한 확인
  boolean validateSessionOwner(Long sessionId, UUID userId);
}
