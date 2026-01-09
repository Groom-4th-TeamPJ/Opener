package spring.backend.domain.exam.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "문제 정답 제출 요청")
public class SubmitAnswerRequest {

    @Schema(description = "입력 혹은 선택한 정답", example = "3", required = true)
    @NotNull(message = "정답은 필수입니다.")
    private Integer selected;

    @Schema(description = "문제 풀이에 소요된 시간 (초)", example = "42", required = true)
    @NotNull(message = "소요 시간은 필수입니다.")
    @PositiveOrZero(message = "소요 시간은 0 이상이어야 합니다.")
    private Integer timeSpent;
}
