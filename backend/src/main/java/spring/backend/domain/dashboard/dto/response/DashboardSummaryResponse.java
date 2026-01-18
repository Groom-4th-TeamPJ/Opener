package spring.backend.domain.dashboard.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "대시보드 응답")
@AllArgsConstructor
@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardSummaryResponse {

    @Schema(description = "총 정답률", example = "85")
    private Integer monthlyAverageCorrectRate;

    @Schema(description = "이번 달 문제 풀이 수", example = "42")
    private Long monthlyQuestionsSolvedCount;

    @Schema(description = "총 학습 시간 일 시간 분", example = "3일 12시간 30분")
    private String totalLearningTimeDesc;

    @Schema(description = "총 풀이 문제 수", example = "256")
    private Long totalQuestionsSolvedCount;


}
