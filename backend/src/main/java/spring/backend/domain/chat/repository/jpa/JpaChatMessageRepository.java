package spring.backend.domain.chat.repository.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.chat.model.entity.ChatMessage;

public interface JpaChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByQuestionResultId(Long questionResultId);

}
