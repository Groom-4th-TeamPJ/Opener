package spring.backend.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Builder;

/**
 * Redis에 저장될 임시 채팅 메시지 DTO
 * <p>
 * Redis Key: chat:session:{sessionId} Redis Value: List<ChatMessageDto> (JSON array) TTL: 24 hours
 * <p>
 * 사용 시나리오: 1. WebSocket으로 실시간 채팅 → Redis에 저장 2. 사용자가 "저장" 버튼 클릭 → PostgreSQL로 마이그레이션
 */
@Builder
public record ChatMessageDto(

        @JsonProperty("sessionId")
        String sessionId,

        // 메시지 역할: USER, LLM
        @JsonProperty("role")
        String role,

        // 메세지 내용
        @JsonProperty("content")
        String content,

        // 메세지 생성 시간
        @JsonProperty("timestamp")
        LocalDateTime timestamp,

        // 메세지 순서
        @JsonProperty("order")
        Integer order
) {
}
