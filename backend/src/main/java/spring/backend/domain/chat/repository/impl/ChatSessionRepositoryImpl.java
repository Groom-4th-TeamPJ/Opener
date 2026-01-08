package spring.backend.domain.chat.repository.impl;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import spring.backend.domain.chat.model.entity.ChatSession;
import spring.backend.domain.chat.repository.jpa.JpaChatSessionRepository;
import spring.backend.domain.chat.repository.spec.ChatSessionRepository;

@Repository
@RequiredArgsConstructor
public class ChatSessionRepositoryImpl implements ChatSessionRepository {

  private final JpaChatSessionRepository jpaChatSessionRepository;

  @Override
  public Optional<ChatSession> findByUserId(UUID userId) {
    return jpaChatSessionRepository.findByUser_Id(userId);
  }

  @Override
  public boolean existsBySessionIdAndUserId(Long sessionId, UUID userId) {
    return jpaChatSessionRepository.existsBySessionIdAndUser_Id(sessionId, userId);
  }

  @Override
  public Optional<ChatSession> findBySessionIdAndUserId(Long sessionId, UUID userId) {
    return jpaChatSessionRepository.findBySessionIdAndUser_Id(sessionId, userId);
  }
}
