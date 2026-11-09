package spring.backend.domain.exam.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.exam.model.entity.QuestionResult;

import java.util.List;
import java.util.UUID;

public interface JpaQuestionResultRepository extends JpaRepository<QuestionResult, Long> {
    boolean existsByExamResultIdAndQuestionId(Long examResultId, Long questionId);

    // 문항 결과의 소유자는 시험 결과의 user_id -> 조인 한 번으로 존재와 소유를 함께 판정
    boolean existsByIdAndExamResultUserId(Long id, UUID userId);

    Page<QuestionResult> findAllByExamResultIdInAndIsOpenerIsTrue(List<Long> examResultIds, Pageable pageable);
}
