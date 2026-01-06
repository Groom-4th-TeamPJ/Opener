package spring.backend.domain.exam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResponse {
    private ExamInfo exam;
    private List<QuestionResponse> questions;
}
