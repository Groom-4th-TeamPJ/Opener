package spring.backend.domain.exam.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.exam.model.entity.QuestionResult;

public interface JpaQuestionResultRepository extends JpaRepository<QuestionResult, Long> {
    boolean existsByExamResultIdAndQuestionId(Long examResultId, Long questionId);
}
