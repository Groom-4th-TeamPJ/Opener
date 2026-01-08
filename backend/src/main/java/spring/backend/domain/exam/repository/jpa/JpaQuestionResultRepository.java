package spring.backend.domain.exam.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.exam.model.entity.QuestionResult;

public interface JpaQuestionResultRepository extends JpaRepository<QuestionResult, Long> {
}
