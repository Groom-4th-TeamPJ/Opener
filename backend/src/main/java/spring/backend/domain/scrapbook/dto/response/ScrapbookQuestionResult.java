package spring.backend.domain.scrapbook.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import spring.backend.domain.exam.model.dto.Passage;
import spring.backend.domain.exam.model.enums.Category;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@AllArgsConstructor
@Builder
public class ScrapbookQuestionResult {
    @Schema(description = "문제 ID", example = "1")
    private Long questionResultId;

    @Schema(description = "문제 유형", example = "기하")
    private Category category;

    @Schema(description = "문제 번호", example = "1")
    private int questionNo;

/*
    @Schema(description = "문제 내용", example = "삼각형 ABC에서 ∠A=60°, ∠B=70°일 때, ∠C의 크기는?")
    private List<Passage> passages;
*/

    @Schema(description = "문제", example = "삼각형 ABC에서 ∠A=60°, ∠B=70°일 때, ∠C의 크기는?")
    private String passage;

    @Schema(description = "분석 일시", example = "2024-05-20T15:30:00")
    private LocalDateTime updatedAt;
}
