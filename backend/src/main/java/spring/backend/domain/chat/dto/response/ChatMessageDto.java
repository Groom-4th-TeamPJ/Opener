package spring.backend.domain.chat.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import spring.backend.domain.chat.dto.enums.ChatRole;

/**
 * 단일 채팅 메시지 DTO
 */
@Builder
public record ChatMessageDto(
        Integer order,           // 메시지 순서 (1부터 시작)
        ChatRole role,           // USER 또는 LLM
        String content,          // 메시지 내용
        LocalDateTime timestamp  // 메시지 생성 시간
) {
}
