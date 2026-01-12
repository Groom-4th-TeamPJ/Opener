package spring.backend.domain.exam.repository.spec;

import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.ExamType;

import java.util.List;
import java.util.Optional;

public interface ExamRepository {
    
    boolean existsById(Long id);

    Optional<Exam> findByExamYearAndExamType(Integer examYear, ExamType examType);

    Optional<Question> findQuestionById(Long id);

    List<Question> findByExamIdAndCategoryInOrderByQuestionNoAsc(Long examId, List<Category> categories);
}
