package spring.backend.domain.chat.repository.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.chat.model.entity.ChatSession;

public interface JpaChatSessionRepository extends JpaRepository<ChatSession, UUID> {
  Optional<ChatSession> findByUser_Id(UUID userId);

  boolean existsBySessionIdAndUser_Id(Long sessionId, UUID userId);

  Optional<ChatSession> findBySessionIdAndUser_Id(Long sessionId, UUID userId);
}
