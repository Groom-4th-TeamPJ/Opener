package spring.backend.domain.scrapbook.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.Page;
import spring.backend.domain.exam.dto.response.ExamInfo;
import spring.backend.domain.exam.dto.response.QuestionResponse;
import spring.backend.domain.exam.model.enums.ExamType;
import spring.backend.shared.response.PageResponse;

@Schema(description = "스크랩북 문제 목록 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ScrapbookResponse {

    @Schema(description = "시험 년도")
    private int examYear;

    @Schema(description = "시험 유형")
    private ExamType examType;

    @Schema(description = "문제 정보")
    private PageResponse<ScrapbookQuestionResult> questionResults;
}
