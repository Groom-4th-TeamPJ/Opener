package spring.backend.domain.chat.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.backend.domain.chat.model.enums.ChatStatus;
import spring.backend.domain.user.model.entity.User;

@Entity
@Table(name = "chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSession {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ChatStatus status;

  @Column(name = "started_at", nullable = false)
  private LocalDateTime startedAt;

  @Column(name = "ended_at")
  private LocalDateTime endedAt;

  // 세션 생성 팩토리 메서드
  public static ChatSession createChatSession(
          User user,
          Long questionId,
          String title
  ) {
    ChatSession chatSession = new ChatSession();
    chatSession.user = user;
    chatSession.status = ChatStatus.ACTIVE;
    chatSession.startedAt = LocalDateTime.now();
    return chatSession;
  }


  // 세션 종료
  public void endChatSession() {
    this.endedAt = LocalDateTime.now();
    this.status = ChatStatus.INACTIVE;
  }
}
