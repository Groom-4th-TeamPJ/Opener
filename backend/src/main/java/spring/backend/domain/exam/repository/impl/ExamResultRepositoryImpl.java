package spring.backend.domain.exam.repository.impl;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.repository.jpa.JpaExamResultRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.domain.scrapbook.dto.response.ScrapbookFilterResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
public class ExamResultRepositoryImpl implements ExamResultRepository {

    private final JpaExamResultRepository jpaExamResultRepository;

    public ExamResultRepositoryImpl(JpaExamResultRepository jpaExamResultRepository) {
        this.jpaExamResultRepository = jpaExamResultRepository;
    }

    @Override
    public Optional<ExamResult> findById(Long id) {
        return jpaExamResultRepository.findById(id);
    }

    @Override
    public Optional<ExamResult> findByIdAndUserId(Long examResultId, UUID userId) {
        return jpaExamResultRepository.findByIdAndUserId(examResultId, userId);
    }

    @Override
    public ExamResult save(ExamResult examResult) {
        return jpaExamResultRepository.save(examResult);
    }

    @Override
    public boolean existsByIdAndUserId(Long id, UUID userId) {
        return jpaExamResultRepository.existsByIdAndUserId(id, userId);
    }

    @Override
    public List<ScrapbookFilterResponse> findScrapbookFiltersByUserId(UUID userId) {
        return jpaExamResultRepository.findScrapbookFiltersByUserId(userId);
    }

    @Override
    public List<ExamResult> findAllByUserIdAndExamIdAndLastOpenerUsageDateIsNotNull(Long examId, UUID userId) {
        return jpaExamResultRepository.findAllByUserIdAndExamIdAndLastOpenerUsageDateIsNotNull(userId, examId);
    }
}
