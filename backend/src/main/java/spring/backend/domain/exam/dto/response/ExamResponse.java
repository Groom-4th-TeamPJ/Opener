package spring.backend.domain.exam.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExamResponse {
    private Long examResultId;
    private ExamInfo exam;
    private List<QuestionResponse> questions;
}
