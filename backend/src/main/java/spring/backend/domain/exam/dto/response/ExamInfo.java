package spring.backend.domain.exam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.backend.domain.exam.model.enums.ExamType;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamInfo {
    private Long examId;
    private Integer examYear;
    private ExamType examType;
    private String name;
    private Integer quantity;
    private Integer timeLimit;
}
