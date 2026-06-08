package spring.backend.domain.chat.repository.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import spring.backend.domain.chat.model.entity.ChatMessage;
import spring.backend.domain.chat.repository.jpa.JpaChatMessageRepository;
import spring.backend.domain.chat.repository.spec.ChatMessageRepository;

// impl 계층 -> 순수 JPA 호출 위에 비즈니스 규칙을 끼워 넣을 자리 확보, spec 과 jpa 사이 완충
@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {

    // JPA 리포지토리에 위임 -> 기본 CRUD 는 Spring Data 가 구현, 중복 코드 제거
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
