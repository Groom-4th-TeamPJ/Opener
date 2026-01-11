package spring.backend.domain.chat.repository.spec;

import spring.backend.domain.chat.model.entity.ChatMessage;

public interface ChatMessageRepository {
    
    ChatMessage save(ChatMessage chatMessage);

}
