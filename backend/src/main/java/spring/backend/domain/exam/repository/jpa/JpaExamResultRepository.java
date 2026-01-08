package spring.backend.domain.exam.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.exam.model.entity.ExamResult;

import java.util.Optional;
import java.util.UUID;

public interface JpaExamResultRepository extends JpaRepository<ExamResult, Long> {

    Optional<ExamResult> findByIdAndUserId(Long id, UUID userId);
}
