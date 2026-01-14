package spring.backend.domain.exam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "문제 정답 제출 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerResponse {

    @Schema(description = "저장된 문제 결과 ID", example = "123")
    Long questionResultId;

    @Schema(description = "정답 여부", example = "true")
    boolean isCorrect;

    @Schema(description = "정답", example = "4")
    int answer;
}
