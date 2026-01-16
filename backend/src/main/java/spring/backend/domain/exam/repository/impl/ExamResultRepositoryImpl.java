package spring.backend.domain.exam.repository.impl;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import spring.backend.domain.exam.model.dto.ExamResultSearchCriteria;
import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.repository.jpa.JpaExamResultRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.domain.scrapbook.dto.response.ScrapbookFilterResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
public class ExamResultRepositoryImpl implements ExamResultRepository {

    private final JpaExamResultRepository jpaExamResultRepository;

    public ExamResultRepositoryImpl(JpaExamResultRepository jpaExamResultRepository) {
        this.jpaExamResultRepository = jpaExamResultRepository;
    }

    @Override
    public Optional<ExamResult> findById(Long id) {
        return jpaExamResultRepository.findById(id);
    }

    @Override
    public Optional<ExamResult> findByIdAndUserId(Long examResultId, UUID userId) {
        return jpaExamResultRepository.findByIdAndUserId(examResultId, userId);
    }

    @Override
    public ExamResult save(ExamResult examResult) {
        return jpaExamResultRepository.save(examResult);
    }

    @Override
    public boolean existsByIdAndUserId(Long id, UUID userId) {
        return jpaExamResultRepository.existsByIdAndUserId(id, userId);
    }

    @Override
    public List<ScrapbookFilterResponse> findScrapbookFiltersByUserId(UUID userId) {
        return jpaExamResultRepository.findScrapbookFiltersByUserId(userId);
    }

    private Specification<ExamResult> toSpecification(UUID userId) {
        return (root, query, builder) -> {
            if (userId == null) {
                return builder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            // ✅ exam 필터는 join으로
            Join<ExamResult, Exam> exam = root.join("exam", JoinType.INNER);

            predicates.add(builder.equal(root.get("userId"), userId));
            predicates.add(builder.isNotNull(root.get("lastOpenerUsageDate")));


            if (query.getResultType() != Long.class) {
                query.orderBy(builder.desc(root.get("lastOpenerUsageDate")));
                query.groupBy(root.get("exam").get("id"));
            }


            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
