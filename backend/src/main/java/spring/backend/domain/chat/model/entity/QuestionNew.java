package spring.backend.domain.chat.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import spring.backend.domain.chat.model.enums.Category;
import spring.backend.domain.chat.model.enums.QuestionType;
import spring.backend.domain.chat.model.vo.Option;
import spring.backend.domain.chat.model.vo.Passage;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.user.model.entity.User;
import spring.backend.shared.entity.BaseEntity;

// TODO: 개발 완료 후 @Entity 활성화 및 Flyway 마이그레이션 파일 생성 필요
// @Entity
public class QuestionNew extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @OneToOne
    private QuestionResult questionResult;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "passages", columnDefinition = "jsonb")
    private List<Passage> passages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private List<Option> options;

    @Column(name = "answer")
    private Integer answer;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private QuestionType questionType; // "MCQ" / "FRQ" 등

    @Column(name = "analysis")
    private String analysis;
}
