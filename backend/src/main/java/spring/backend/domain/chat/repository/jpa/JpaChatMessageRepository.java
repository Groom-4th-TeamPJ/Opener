package spring.backend.domain.chat.repository.jpa;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.backend.domain.chat.model.entity.ChatMessage;

// JpaRepository 확장 -> 기본 CRUD/페이징 자동 제공, 구현 클래스 작성 불필요
public interface JpaChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 메서드 이름으로 쿼리 자동 생성 -> JPQL 직접 작성 없이 questionResultId 단건 조회
    // Optional 반환 -> 결과 없음을 null 대신 명시적으로 표현해 NPE 방지
    Optional<ChatMessage> findByQuestionResultId(Long questionResultId);

    // id 만 읽는다 -> messages(jsonb) 본문을 끌어오지 않고, V18 부분 인덱스(created_at WHERE summary IS NULL)로 대상만 훑는다
    @Query("select m.id from ChatMessage m where m.summary is null and m.createdAt < :cutoff order by m.id asc")
    List<Long> findPendingSummaryIds(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

}
