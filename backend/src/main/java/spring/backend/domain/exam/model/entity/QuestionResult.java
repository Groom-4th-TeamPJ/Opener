package spring.backend.domain.exam.model.entity;

import jakarta.persistence.*;
import lombok.*;
import spring.backend.shared.entity.BaseEntity;

@Entity
@Table(
        name = "question_results",
        uniqueConstraints = @UniqueConstraint(name="uk_exam_result_question", columnNames = {"exam_result_id", "question_id"}),
        indexes = {
                @Index(name="idx_exam_result", columnList="exam_result_id"),
                @Index(name="idx_question", columnList="question_id"),
                @Index(name="idx_exam", columnList="exam_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name="exam_result_id", nullable=false)
    private ExamResult examResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "selected", nullable = false)
    private Integer selected;

    @Column(name = "is_correct", nullable = false)
    private boolean isCorrect;

    @Column(name ="time_spent", nullable = false)
    private Integer timeSpent;

    @Column(name = "is_opener", nullable = false)
    private boolean isOpener;

    public static QuestionResult of(ExamResult er, Question q, int selected, int timeSpent) {
        QuestionResult qr = new QuestionResult();
        qr.examResult = er;
        qr.question = q;
        qr.selected = selected;
        qr.isCorrect = false;
        qr.timeSpent = timeSpent;
        qr.isOpener = false;
        return qr;
    }

    public void markCorrect(boolean correct) {
        this.isCorrect = correct;
    }

    public void markOpener() {
        this.isOpener = true;
    }
}
