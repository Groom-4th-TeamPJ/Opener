package spring.backend.domain.chat.dto.redis_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Builder;
import spring.backend.domain.chat.dto.enums.ChatRole;

@Builder
public record MessageDto(

        // 메세지 주체
        @JsonProperty("role")
        ChatRole chatRole,

        // 메세지 내용
        @JsonProperty("message")
        String message,

        // 메세지 생성 시간
        @JsonProperty("timestamp")
        LocalDateTime timestamp
) {
}
