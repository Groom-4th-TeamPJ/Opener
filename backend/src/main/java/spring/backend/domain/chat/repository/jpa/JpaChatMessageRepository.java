package spring.backend.domain.chat.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.chat.model.entity.ChatMessage;

public interface JpaChatMessageRepository extends JpaRepository<ChatMessage, Long> {

}
