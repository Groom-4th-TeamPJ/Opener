package spring.backend.domain.chat.repository.spec;

import spring.backend.domain.chat.model.entity.QuestionNew;

public interface QuestionNewRepository {

    /**
     * 변형 문제 저장
     */
    QuestionNew save(QuestionNew questionNew);
}
