package spring.backend.domain.chat.dto.response;

import java.util.List;
import lombok.Builder;

/**
 * 채팅 히스토리 응답 DTO (내부 호출용)
 */
@Builder
public record ChatHistoryResponse(
        List<ChatMessageDto> chat,  // 채팅 메시지 목록
        String summary              // 대화 요약
) {
}
