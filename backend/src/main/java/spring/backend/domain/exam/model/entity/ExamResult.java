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

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(name = "total_time_spent", nullable = false)
    private Integer totalTimeSpent;

    public static ExamResult of(UUID userId, Long examId) {
        ExamResult er = new ExamResult();
        er.userId = userId;
        er.examId = examId;
        er.totalScore = 0;
        er.totalTimeSpent = 0;
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
}
