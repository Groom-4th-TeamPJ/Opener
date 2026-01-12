package spring.backend.domain.chat.repository.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import spring.backend.domain.chat.model.entity.ChatMessage;
import spring.backend.domain.chat.repository.jpa.JpaChatMessageRepository;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    private final JpaChatMessageRepository jpaChatMessageRepository;

    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        return jpaChatMessageRepository.save(chatMessage);
    }

}
