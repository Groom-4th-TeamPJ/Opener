package spring.backend.domain.chat.repository.spec;

import java.util.Optional;
import spring.backend.domain.chat.model.entity.ChatMessage;

public interface ChatMessageRepository {

    ChatMessage save(ChatMessage chatMessage);

    Optional<ChatMessage> findByQuestionResultId(Long questionResultId);

}
