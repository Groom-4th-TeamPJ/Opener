package spring.backend.domain.chat.service.spec;

import java.util.List;
import java.util.UUID;
import spring.backend.domain.chat.dto.response.ChatMessageDto;

public interface ChatRedisService {

  /**
   * 세션 초기화
   *
   * @param sessionId  세션 ID
   * @param userId     사용자 ID
   * @param questionId 질문 ID
   */
  void initializeSession(Long sessionId, UUID userId, Long questionId);

  /**
   * 메시지 저장
   *
   * @param sessionId 세션 ID
   * @param message   메시지
   */
  void saveMessage(String sessionId, ChatMessageDto message);

  /**
   * 세션의 모든 메시지 조회
   *
   * @param sessionId 세션 ID
   * @return 메시지 리스트
   */
  List<ChatMessageDto> getSessionMessages(String sessionId);

  /**
   * 세션 소유자 ID 조회
   *
   * @param sessionId 세션 ID
   * @return 사용자 ID
   */
  UUID getSessionOwnerId(String sessionId);

  /**
   * 세션 삭제
   *
   * @param sessionId 세션 ID
   */
  void deleteSession(String sessionId);
}
