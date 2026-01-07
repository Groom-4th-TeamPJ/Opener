package spring.backend.domain.exam.model.entity;


import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import spring.backend.domain.exam.model.converter.OptionsConverter;
import spring.backend.domain.exam.model.converter.PassagesConverter;
import spring.backend.domain.exam.model.dto.Option;
import spring.backend.domain.exam.model.dto.Passage;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.QuestionType;
import spring.backend.shared.entity.BaseEntity;

import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_no")
    private Integer questionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Category category;

    @Column(name = "point")
    private Integer point;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type")
    private QuestionType questionType; // "MCQ" / "FRQ" 등

    // JSONB 저장 (Postgres 사용 시 columnDefinition = "jsonb")
    @Column(name = "passages", columnDefinition = "jsonb")
    @Convert(converter = PassagesConverter.class)
    private List<Passage> passages;

    @Column(name = "options", columnDefinition = "jsonb")
    @Convert(converter = OptionsConverter.class)
    private List<Option> options;

    @Column(name = "answer")
    private Integer answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    @JsonBackReference
    private Exam exam;
}
