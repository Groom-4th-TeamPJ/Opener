package spring.backend.domain.exam.repository.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.exam.repository.jpa.JpaQuestionResultRepository;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class QuestionResultRepositoryImpl implements QuestionResultRepository {

    private final JpaQuestionResultRepository jpaQuestionResultRepository;

    public QuestionResultRepositoryImpl(JpaQuestionResultRepository jpaQuestionResultRepository) {
        this.jpaQuestionResultRepository = jpaQuestionResultRepository;
    }

    @Override
    public Optional<QuestionResult> findById(Long id) {
        return jpaQuestionResultRepository.findById(id);
    }

    @Override
    public QuestionResult save(QuestionResult questionResult) {
        return jpaQuestionResultRepository.save(questionResult);
    }

    @Override
    public boolean existsByExamResultIdAndQuestionId(Long examResultId, Long questionId) {
        return jpaQuestionResultRepository.existsByExamResultIdAndQuestionId(examResultId, questionId);
    }

    @Override
    public Page<QuestionResult> findAllByExamResultIdInAndIsOpenerIsTrue(List<Long> examResultIds, Pageable pageable) {
        return jpaQuestionResultRepository.findAllByExamResultIdInAndIsOpenerIsTrue(examResultIds, pageable);
    }
}
