package spring.backend.domain.exam.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "시험 결과 요약 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResultSummaryResponse {
    @Schema(description = "총 정답 수", example = "50")
    private int correctCount;

    @Schema(description = "총 오답 수", example = "60")
    private int incorrectCount;

    @Schema(description = "오프너 분석 수", example = "45")
    private int openerUsageCount;

    @Schema(description = "정답률", example = "90")
    private long totalTimeSpent;
}
