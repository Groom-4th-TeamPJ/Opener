package spring.backend.domain.chat.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import spring.backend.domain.chat.model.entity.QuestionNew;
import spring.backend.domain.chat.repository.jpa.JpaQuestionNewRepository;
import spring.backend.domain.chat.repository.spec.QuestionNewRepository;

/**
 * QuestionNew Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class QuestionNewRepositoryImpl implements QuestionNewRepository {

    private final JpaQuestionNewRepository jpaQuestionNewRepository;

    @Override
    public QuestionNew save(QuestionNew questionNew) {
        return jpaQuestionNewRepository.save(questionNew);
    }
}
