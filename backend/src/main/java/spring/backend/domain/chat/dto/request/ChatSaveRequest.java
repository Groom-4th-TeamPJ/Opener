package spring.backend.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatSaveRequest(

        @NotBlank(message = "sessionId 가 필요합니다.")
        Long sessionId,

        @NotBlank(message = "questionResultId 가 필요합니다.")
        Long questionResultId

) implements SessionScoped {
}
