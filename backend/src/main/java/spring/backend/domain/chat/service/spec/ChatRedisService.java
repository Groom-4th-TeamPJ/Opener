package spring.backend.domain.chat.service.spec;

import java.util.List;
import java.util.UUID;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;

public interface ChatRedisService {

    // 세션 초기화
    void initializeSession(Long sessionId, UUID userId);

    // 메시지 저장
    void saveMessage(Long sessionId, RedisMessageDto message);

    // 세션의 모든 메시지 조회 (영속화용 — 전량)
    List<RedisMessageDto> getSessionMessages(Long sessionId);

    // 세션의 최근 메시지 조회 (LLM 컨텍스트용 — 윈도우 적용)
    List<RedisMessageDto> getRecentSessionMessages(Long sessionId);

    // 세션 삭제
    void deleteSession(Long sessionId);

    // 마지막으로 저장한 메시지 하나 제거 (답을 받지 못한 요청 메시지 되돌리기용)
    void removeLastMessage(Long sessionId);

    // 메세지 삭제
    void deleteMessage(Long sessionId);

    // 세션 권한 확인
    void validateSessionOwner(Long sessionId, UUID userId);
}
