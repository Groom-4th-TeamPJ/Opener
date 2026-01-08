package spring.backend.domain.exam.dto.request;

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
public class SubmitAnswerRequest {

    @NotNull(message = "정답은 필수입니다.")
    private Integer selected;

    @NotNull(message = "소요 시간은 필수입니다.")
    @PositiveOrZero(message = "소요 시간은 0 이상이어야 합니다.")
    private Integer timeSpent;
}
