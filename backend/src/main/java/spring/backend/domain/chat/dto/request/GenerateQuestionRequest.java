package spring.backend.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 변형 문제 생성 요청 DTO
 */
public record GenerateQuestionRequest(
        @NotNull(message = "문제 ID는 필수입니다")
        Long questionId,

        @NotNull(message = "시험 결과 ID는 필수입니다")
        Long questionResultId
) {
}
