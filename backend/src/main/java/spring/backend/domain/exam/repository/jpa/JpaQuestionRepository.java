package spring.backend.domain.exam.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.enums.Category;

import java.util.List;

public interface JpaQuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByExamIdAndCategoryOrderByOrderAsc(Long examId, Category category);
}
