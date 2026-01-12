package spring.backend.domain.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SseMessageResponse(
        String type,           // "chunk", "complete", "error"
        String sessionId,
        String chunk,          // for "chunk" type
        String error           // for "error" type
) {
}
