package spring.backend.domain.chat.dto.redis_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Builder;
import spring.backend.domain.chat.dto.enums.ChatRole;

// record -> 불변 + equals/hashCode 자동, Redis 직렬화 단위로 안전
// @Builder -> 필드 많을 때 가독성 있는 생성, 순서 실수 방지
@Builder
public record RedisMessageDto(

        // @JsonProperty -> 자바 필드명(chatRole)과 JSON 키(role)를 분리, 저장 포맷을 안정적으로 고정
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
