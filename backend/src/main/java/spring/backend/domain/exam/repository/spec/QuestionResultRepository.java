package spring.backend.domain.exam.repository.spec;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import spring.backend.domain.exam.model.entity.QuestionResult;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionResultRepository {
    Optional<QuestionResult> findById(Long id);

    QuestionResult save(QuestionResult questionResult);

    boolean existsByExamResultIdAndQuestionId(Long examResultId, Long questionId);

    boolean existsByIdAndExamResultUserId(Long id, UUID userId);

    Page<QuestionResult> findAllByExamResultIdInAndIsOpenerIsTrue(List<Long> examResultIds, Pageable pageable);
}
