package spring.backend.domain.chat.dto.message_dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.UUID;
import lombok.Builder;

@Builder
public record ChatMessageSaveEvent(
        @JsonProperty("sessionId")
        Long sessionId,

        @JsonProperty("userId")
        UUID userId
) implements Serializable {
}
