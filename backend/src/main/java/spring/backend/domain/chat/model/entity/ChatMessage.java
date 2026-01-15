package spring.backend.domain.chat.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.shared.entity.BaseEntity;

@Entity
@Table(name = "chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "question_result_id", nullable = false)
    private QuestionResult questionResult;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "messages", columnDefinition = "jsonb", nullable = false)
    private List<ChatMessageContent> messages = new ArrayList<>();

    // chat message 생성 팩토리 메서드
    public static ChatMessage createFromSession(
            QuestionResult questionResult,
            List<ChatMessageContent> messages
    ) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.questionResult = questionResult;
        chatMessage.messages = messages != null ? messages : new ArrayList<>();
        return chatMessage;
    }

}
