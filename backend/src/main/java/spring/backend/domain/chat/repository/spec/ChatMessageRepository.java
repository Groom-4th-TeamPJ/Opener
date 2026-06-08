package spring.backend.domain.chat.repository.spec;

import java.util.Optional;
import spring.backend.domain.chat.model.entity.ChatMessage;

// spec 인터페이스 -> 서비스는 추상에만 의존, JPA 교체/테스트 대역 주입이 자유로움
public interface ChatMessageRepository {

    ChatMessage save(ChatMessage chatMessage);

    Optional<ChatMessage> findByQuestionResultId(Long questionResultId);

}
