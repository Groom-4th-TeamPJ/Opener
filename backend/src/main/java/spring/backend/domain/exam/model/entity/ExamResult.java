package spring.backend.domain.exam.model.entity;

import jakarta.persistence.*;
import lombok.*;
import spring.backend.shared.entity.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "exam_results",
        indexes = {
                @Index(
                        name = "idx_exam_results_user_exam",
                        columnList = "user_id, exam_id"
                ),
                @Index(
                        name = "idx_exam_results_user",
                        columnList = "user_id"
                )
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ExamResult extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(name = "total_time_spent", nullable = false)
    private Integer totalTimeSpent;

    @Column(name = "correct_count", nullable = false)
    private Integer correctCount;

    @Column(name = "incorrect_count", nullable = false)
    private Integer incorrectCount;

    @Column(name = "opener_usage_count", nullable = false)
    private Integer openerUsageCount;

    public static ExamResult of(UUID userId, Exam exam) {
        ExamResult er = new ExamResult();
        er.userId = userId;
        er.exam = exam;
        er.totalScore = 0;
        er.totalTimeSpent = 0;
        er.correctCount = 0;
        er.incorrectCount = 0;
        er.openerUsageCount = 0;
        return er;
    }

    public void addScore(int score) {
        if (score < 0) {
            throw new IllegalArgumentException("점수는 음수 불가");
        }
        this.totalScore += score;
    }

    public void addTimeSpent(int time) {
        if (time < 0) {
            throw new IllegalArgumentException("시간은 음수 불가");
        }
        this.totalTimeSpent += time;
    }

    public void incrementCorrectCount() {
        this.correctCount += 1;
    }

    public void incrementIncorrectCount() {
        this.incorrectCount += 1;
    }

    public void incrementOpenerUsageCount() {
        this.openerUsageCount += 1;
    }
}
