package spring.backend.domain.chat.service.spec;

import java.util.List;
import java.util.UUID;
import spring.backend.domain.chat.dto.redis_dto.RedisMessageDto;

public interface ChatRedisService {

    // 세션 초기화
    void initializeSession(Long sessionId, UUID userId);

    // 메시지 저장 -> push 후 목록 길이를 돌려준다, 서킷이 저장을 버렸으면 -1
    long saveMessage(Long sessionId, RedisMessageDto message);

    // 세션의 모든 메시지 조회 (영속화용 — 전량)
    List<RedisMessageDto> getSessionMessages(Long sessionId);

    // 방금 저장한 메시지 직전까지의 최근 메시지 조회 (LLM 컨텍스트용 — 윈도우 적용)
    // writtenLength = saveMessage 가 돌려준 길이, 음수면 저장이 버려진 턴
    List<RedisMessageDto> getHistoryBefore(Long sessionId, long writtenLength);

    // 세션 삭제
    void deleteSession(Long sessionId);

    // 방금 저장하려던 메시지가 마지막 항목일 때만 그 하나를 제거 (답을 받지 못한 요청 메시지 되돌리기용)
    void removeLastMessage(Long sessionId, RedisMessageDto expected);

    // 메세지 삭제
    void deleteMessage(Long sessionId);

    // 세션 권한 확인
    void validateSessionOwner(Long sessionId, UUID userId);
}
