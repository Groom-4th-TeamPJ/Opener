package spring.backend.domain.exam.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.exam.model.entity.QuestionResult;

import java.util.List;

public interface JpaQuestionResultRepository extends JpaRepository<QuestionResult, Long> {
    boolean existsByExamResultIdAndQuestionId(Long examResultId, Long questionId);

    Page<QuestionResult> findAllByExamResultIdInAndIsOpenerIsTrue(List<Long> examResultIds, Pageable pageable);
}
