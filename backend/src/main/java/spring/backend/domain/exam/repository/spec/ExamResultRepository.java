package spring.backend.domain.exam.repository.spec;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import spring.backend.domain.exam.model.dto.ExamResultSearchCriteria;
import spring.backend.domain.exam.model.entity.ExamResult;

import java.util.Optional;
import java.util.UUID;

public interface ExamResultRepository {

    boolean existsByIdAndUserId(Long id, UUID userId);

    Optional<ExamResult> findById(Long id);

    Optional<ExamResult> findByIdAndUserId(Long examResultId, UUID userId);

    ExamResult save(ExamResult examResult);

    Page<ExamResult> searchExamResults(ExamResultSearchCriteria criteria, Pageable pageable);


}
