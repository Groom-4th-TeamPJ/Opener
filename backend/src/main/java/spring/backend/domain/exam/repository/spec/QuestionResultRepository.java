package spring.backend.domain.exam.repository.spec;

import spring.backend.domain.exam.model.entity.QuestionResult;

import java.util.Optional;

public interface QuestionResultRepository {
    Optional<QuestionResult> findById(Long id);

    QuestionResult save(QuestionResult questionResult);
}
