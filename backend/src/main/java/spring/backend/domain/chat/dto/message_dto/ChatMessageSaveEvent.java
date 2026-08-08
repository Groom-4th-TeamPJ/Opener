package spring.backend.domain.chat.dto.message_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;

// implements Serializable -> RabbitMQ 전송 위해 직렬화 가능해야 함
// id 만 담는 경량 이벤트 -> 큐에 대용량 본문 안 싣고 컨슈머가 Redis 에서 실데이터 조회
// publishedAt 은 버퍼가 비었을 때 정상 만료와 비정상 소실을 가르는 유일한 근거
@Builder
public record ChatMessageSaveEvent(
        @JsonProperty("sessionId")
        Long sessionId,

        @JsonProperty("userId")
        UUID userId,

        @JsonProperty("questionResultId")
        Long questionResultId,

        @JsonProperty("publishedAt")
        Instant publishedAt
) implements Serializable {
}
