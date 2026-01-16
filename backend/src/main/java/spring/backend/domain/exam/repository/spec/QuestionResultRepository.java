package spring.backend.domain.exam.repository.spec;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import spring.backend.domain.exam.model.entity.QuestionResult;

import java.util.List;
import java.util.Optional;

public interface QuestionResultRepository {
    Optional<QuestionResult> findById(Long id);

    QuestionResult save(QuestionResult questionResult);

    boolean existsByExamResultIdAndQuestionId(Long examResultId, Long questionId);

    Page<QuestionResult> findAllByExamResultIdInAndIsOpenerIsTrue(List<Long> examResultIds, Pageable pageable);
}
