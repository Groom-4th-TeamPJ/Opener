package spring.backend.domain.exam.repository.spec;

import spring.backend.domain.exam.dto.response.ExamResponse;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.ExamType;

import java.util.Optional;

public interface ExamRepository {
    Optional<ExamResponse> findExamWithQuestions(Integer examYear, ExamType examType, Category category);
}
