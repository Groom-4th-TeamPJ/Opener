package spring.backend.domain.exam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerResponse {

    @Schema(description = "저장된 문제 결과 ID", example = "123")
    Long questionResultId;

    @Schema(description = "정답 여부", example = "true")
    boolean isCorrect;
}
