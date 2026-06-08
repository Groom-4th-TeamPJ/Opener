package spring.backend.domain.chat.repository.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.chat.model.entity.ChatMessage;

// JpaRepository 확장 -> 기본 CRUD/페이징 자동 제공, 구현 클래스 작성 불필요
public interface JpaChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 메서드 이름으로 쿼리 자동 생성 -> JPQL 직접 작성 없이 questionResultId 단건 조회
    // Optional 반환 -> 결과 없음을 null 대신 명시적으로 표현해 NPE 방지
    Optional<ChatMessage> findByQuestionResultId(Long questionResultId);

}
