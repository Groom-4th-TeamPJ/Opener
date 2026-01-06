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

import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // DB 컬럼명이 "order"인 경우
    @Column(name = "\"order\"")
    private Integer order;

    @Column(name = "category")
    private Category category;

    @Column(name = "point")
    private Integer point;

    @Column(name = "type")
    private QuestionType type; // "MCQ" / "FRQ" 등

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
