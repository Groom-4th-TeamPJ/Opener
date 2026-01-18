package spring.backend.domain.scrapbook.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.ExamType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "스크랩북 분류 목록 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ScrapbookFilterResponse {
    @Schema(description = "문제 ID", example = "1")
    private Long examId;
    @Schema(description = "문제 년도", example = "2026")
    private int examYear;
    @Schema(description = "문제 유형", example = "CSAT")
    private ExamType examType;
    @Schema(description = "오프너 분석 수량", example = "1")
    private Long openerUsageCount;
    @Schema(description = "최근 오프너 분석 일자", example = "2024-05-20")
    private LocalDateTime lastOpenerUsageDate;
}
