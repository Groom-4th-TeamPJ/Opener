package spring.backend.domain.chat.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChatSendRequest(
        @NotBlank(message = "세션 아이디가 필요합니다.")
        Long sessionId,

        @NotBlank(message = "문제 아이디가 필요합니다.")
        Long questionId,

        @NotBlank(message = "내용을 필수로 입력해주세요")
        String message
) {

}
