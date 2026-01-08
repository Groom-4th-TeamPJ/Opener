package spring.backend.domain.exam.repository.impl;

import org.springframework.stereotype.Repository;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.repository.jpa.JpaExamResultRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;

import java.util.Optional;
import java.util.UUID;

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
}
