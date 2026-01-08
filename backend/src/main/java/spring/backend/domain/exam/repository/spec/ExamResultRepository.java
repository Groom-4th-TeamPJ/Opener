package spring.backend.domain.exam.repository.spec;

import spring.backend.domain.exam.model.entity.ExamResult;

import java.util.Optional;
import java.util.UUID;

public interface ExamResultRepository {

    Optional<ExamResult> findById(Long id);

    Optional<ExamResult> findByIdAndUserId(Long examResultId, UUID userId);

    ExamResult save(ExamResult examResult);


}
