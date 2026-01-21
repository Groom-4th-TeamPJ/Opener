package spring.backend.domain.chat.repository.impl;

import java.util.Optional;
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

    @Override
    public Optional<ChatMessage> findByQuestionResultId(Long questionResultId) {
        return jpaChatMessageRepository.findByQuestionResultId(questionResultId);
    }

}
