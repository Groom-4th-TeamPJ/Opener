package spring.backend.domain.chat.model.entity;

import jakarta.persistence.*;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import spring.backend.domain.chat.model.enums.Category;
import spring.backend.domain.chat.model.enums.QuestionType;
import spring.backend.domain.chat.model.vo.Option;
import spring.backend.domain.chat.model.vo.Passage;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.user.model.entity.User;
import spring.backend.shared.entity.BaseEntity;

/**
 * AI가 생성한 변형 문제 엔티티
 *
 * RAG 기반으로 원본 문제를 참조하여 생성된 유사 문제를 저장합니다.
 * 각 변형 문제는 사용자와 특정 시험 결과(QuestionResult)에 연결됩니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "question_new")
public class QuestionNew extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_result_id", nullable = false)
    private QuestionResult questionResult;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "passages", columnDefinition = "jsonb", nullable = false)
    private List<Passage> passages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private List<Option> options;

    @Column(name = "answer")
    private Integer answer;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false)
    private QuestionType questionType; // "MCQ" / "FRQ" 등

    @Column(name = "analysis", columnDefinition = "TEXT")
    private String analysis;

    /**
     * 변형 문제 생성 정적 팩토리 메서드
     *
     * @param user 문제를 요청한 사용자
     * @param questionResult 원본 문제 결과
     * @param passages 생성된 문제 지문
     * @param options 생성된 선택지
     * @param answer 정답 번호
     * @param category 문제 카테고리
     * @param questionType 문제 유형
     * @param analysis 풀이 방법
     * @return 생성된 QuestionNew 엔티티
     */
    public static QuestionNew create(
            User user,
            QuestionResult questionResult,
            List<Passage> passages,
            List<Option> options,
            Integer answer,
            Category category,
            QuestionType questionType,
            String analysis
    ) {
        QuestionNew questionNew = new QuestionNew();
        questionNew.user = user;
        questionNew.questionResult = questionResult;
        questionNew.passages = passages;
        questionNew.options = options;
        questionNew.answer = answer;
        questionNew.category = category;
        questionNew.questionType = questionType;
        questionNew.analysis = analysis;
        return questionNew;
    }
}
