package spring.backend.domain.scrapbook.dto.response.detail;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import spring.backend.domain.chat.dto.response.ChatHistoryResponse;
import spring.backend.domain.chat.dto.response.ChatMessageDto;
import spring.backend.domain.exam.dto.response.ExamInfo;
import spring.backend.domain.exam.model.dto.Option;
import spring.backend.domain.exam.model.dto.Passage;
import spring.backend.domain.exam.model.enums.ExamType;
import spring.backend.domain.exam.model.enums.QuestionType;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "스크랩북 상세보기 응답")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ScrapbookDetailResponse {

    // Exam
    // examYear, examType
    @Schema(description = "시험 년도")
    private int examYear;

    @Schema(description = "시험 유형")
    private ExamType examType;

    // Question
    // passages, options, answer, point, questionNo, questionType
    @Schema(description = "문제 내용", example = "삼각형 ABC에서 ∠A=60°, ∠B=70°일 때, ∠C의 크기는?")
    private List<Passage> passages;

    @Schema(description = "보기 목록")
    private List<Option> options;

    @Schema(description = "정답", example = "3")
    private int answer;

    @Schema(description = "배점", example = "5")
    private int point;

    @Schema(description = "문제 번호", example = "1")
    private int questionNo;

    @Schema(description = "문제 유형", example = "FRQ, MCQ")
    private QuestionType questionType;

    // QuestionResult
    // selected, isCorrect, createdAt
    @Schema(description = "문제 결과 ID", example = "1")
    private Long questionResultId;

    @Schema(description = "선택한 보기", example = "2")
    private int selected;

    @Schema(description = "정답 여부", example = "true")
    private boolean isCorrect;

    @Schema(description = "문제 풀이 일시", example = "2024-05-20T15:30:00")
    private LocalDateTime createdAt;

    // chat
    @Schema(description = "챗 히스토리")
    private List<ChatMessageDto> chat;
    // prompt, response
    @Schema(description = "프롬포트 요약")
    private String promptSummary;

}
