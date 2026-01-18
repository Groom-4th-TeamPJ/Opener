package spring.backend.domain.exam.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.scrapbook.dto.response.ScrapbookFilterResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaExamResultRepository extends JpaRepository<ExamResult, Long>, JpaSpecificationExecutor<ExamResult> {

    Optional<ExamResult> findByIdAndUserId(Long id, UUID userId);

    boolean existsByIdAndUserId(Long id, UUID userId);

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
