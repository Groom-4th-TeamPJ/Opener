package spring.backend.domain.chat.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.sql.Timestamp;
import java.util.UUID;
import lombok.Setter;
import spring.backend.domain.chat.model.enums.ChatStatus;
import spring.backend.domain.user.model.entity.User;

@Entity
public class ChatSession {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinColumn(nullable = false, unique = true)
  @Setter
  private User user;

  @Enumerated
  @Column
  private ChatStatus status;

  @Column
  private Timestamp startedAt;

  @Column
  private Timestamp endedAt;

  public static ChatSession createChatSession(User user) {
    ChatSession chatSession = new ChatSession();
    chatSession.user = user;
    chatSession.status = ChatStatus.ACTIVE;
    chatSession.startedAt = Timestamp.valueOf(java.time.LocalDateTime.now());
    return chatSession;
  }

  public void endChatSession() {
    this.endedAt = Timestamp.valueOf(java.time.LocalDateTime.now());
    this.status = ChatStatus.INACTIVE;
  }
}
