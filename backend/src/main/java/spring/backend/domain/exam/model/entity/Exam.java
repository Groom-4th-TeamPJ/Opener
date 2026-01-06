package spring.backend.domain.exam.model.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import spring.backend.domain.exam.model.enums.ExamType;
import spring.backend.shared.entity.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exams")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Exam extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // DB에 "year"로 저장되어 있는 경우를 위해 컬럼명에 따옴표 유지
    @Column(name = "\"year\"")
    private Integer year;

    @Column(name = "exam_type")
    private ExamType examType;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "time_limit")
    private Integer timeLimit;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<Question> questions = new ArrayList<>();
}
