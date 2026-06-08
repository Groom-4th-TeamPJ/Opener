package spring.backend.domain.chat.dto.message_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.UUID;
import lombok.Builder;

// implements Serializable -> RabbitMQ 전송 위해 직렬화 가능해야 함
// id 만 담는 경량 이벤트 -> 큐에 대용량 본문 안 싣고 컨슈머가 Redis 에서 실데이터 조회
@Builder
public record ChatMessageSaveEvent(
        @JsonProperty("sessionId")
        Long sessionId,

        @JsonProperty("userId")
        UUID userId,

        @JsonProperty("questionResultId")
        Long questionResultId
) implements Serializable {
}
