package spring.backend.domain.exam.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.repository.dto.DashboardStatsRow;
import spring.backend.domain.scrapbook.dto.response.ScrapbookFilterResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaExamResultRepository extends JpaRepository<ExamResult, Long>, JpaSpecificationExecutor<ExamResult> {

    Optional<ExamResult> findByIdAndUserId(Long id, UUID userId);

    boolean existsByIdAndUserId(Long id, UUID userId);


    @Query(value = """
            WITH bounds AS (
                SELECT date_trunc('month', now()) AS month_start,
                       date_trunc('month', now()) + interval '1 month' AS month_end
            )
            SELECT SUM(er.total_time_spent) AS total_time_spent_seconds,
                   SUM(er.correct_count) + SUM(er.incorrect_count) AS total_questions_solved_count,
                   SUM(er.correct_count + er.incorrect_count) FILTER (
                    WHERE er.created_at >= b.month_start AND er.created_at < b.month_end
                    ) AS month_questions_solved_count,
                   SUM(er.correct_count) FILTER (
                            WHERE er.created_at >= b.month_start AND er.created_at < b.month_end
                   ) AS month_correct_count
              FROM exam_results er
              JOIN bounds b ON true
             WHERE  er.user_id = :userId
             GROUP BY er.user_id
            """, nativeQuery = true
    )
    DashboardStatsRow getDashboardSummary(UUID userId);

    @Query("""     
        SELECT new spring.backend.domain.scrapbook.dto.response.ScrapbookFilterResponse (
               e.id,
               e.examYear,
               e.examType,
               SUM(er.openerUsageCount),
               MAX(er.lastOpenerUsageDate)
         )
         FROM ExamResult er
         JOIN er.exam e
         WHERE er.userId = :userId
           AND er.lastOpenerUsageDate IS NOT NULL
           AND er.openerUsageCount > 0
      GROUP BY e.id, e.examYear, e.examType
      ORDER BY MAX(er.lastOpenerUsageDate) DESC
    """)
    List<ScrapbookFilterResponse> findScrapbookFiltersByUserId(UUID userId);

    List<ExamResult> findAllByUserIdAndExamIdAndLastOpenerUsageDateIsNotNull(UUID userId, Long examId);
}
