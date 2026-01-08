package spring.backend.domain.chat.repository.spec;

import java.util.Optional;
import java.util.UUID;
import spring.backend.domain.chat.model.entity.ChatSession;

public interface ChatSessionRepository {

  Optional<ChatSession> findByUserId(UUID userId);

  boolean existsBySessionIdAndUserId(Long sessionId, UUID userId);

  Optional<ChatSession> findBySessionIdAndUserId(Long sessionId, UUID userId);

}
