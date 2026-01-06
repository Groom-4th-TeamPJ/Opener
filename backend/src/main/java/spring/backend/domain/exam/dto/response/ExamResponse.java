package spring.backend.domain.exam.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExamResponse {
    private ExamInfo exam;
    private List<QuestionResponse> questions;
}
