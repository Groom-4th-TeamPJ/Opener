package spring.backend.domain.exam.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import spring.backend.domain.exam.model.entity.ExamResult;

import java.util.Optional;
import java.util.UUID;

public interface JpaExamResultRepository extends JpaRepository<ExamResult, Long>, JpaSpecificationExecutor<ExamResult> {

    Optional<ExamResult> findByIdAndUserId(Long id, UUID userId);

    boolean existsByIdAndUserId(Long id, UUID userId);
}
