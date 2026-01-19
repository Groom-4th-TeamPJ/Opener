package spring.backend.domain.chat.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.chat.model.entity.QuestionNew;

/**
 * QuestionNew JPA Repository
 */
public interface JpaQuestionNewRepository extends JpaRepository<QuestionNew, Long> {
}
