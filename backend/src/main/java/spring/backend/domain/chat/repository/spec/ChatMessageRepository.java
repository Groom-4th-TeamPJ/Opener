package spring.backend.domain.chat.repository.spec;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import spring.backend.domain.chat.model.entity.ChatMessage;

// spec 인터페이스 -> 서비스는 추상에만 의존, JPA 교체/테스트 대역 주입이 자유로움
public interface ChatMessageRepository {

    ChatMessage save(ChatMessage chatMessage);

    Optional<ChatMessage> findByQuestionResultId(Long questionResultId);

    Optional<ChatMessage> findById(Long id);

    // 요약이 아직 없고 cutoff 보다 먼저 저장된 행의 id -> 오래된 id 부터 limit 개, 요약 이벤트를 다시 내는 점검용
    List<Long> findPendingSummaryIds(LocalDateTime cutoff, int limit);

}
