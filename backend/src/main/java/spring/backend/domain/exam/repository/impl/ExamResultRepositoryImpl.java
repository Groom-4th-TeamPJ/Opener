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
    public Page<ExamResult> searchExamResults(ExamResultSearchCriteria criteria, Pageable pageable) {
        log.info("ExamResultRepositoryImpl.searchExamResults - criteria: {}, pageable: {}", criteria, pageable);
        Specification<ExamResult> specification = toSpecification(criteria);
        return jpaExamResultRepository.findAll(specification, pageable);
    }

    private Specification<ExamResult> toSpecification(ExamResultSearchCriteria criteria) {
        return (root, query, builder) -> {
            if (criteria == null) {
                return builder.conjunction();
            }
            // ✅ fetch join (단, count 쿼리일 땐 제외)
            if (query.getResultType() != Long.class) {
                root.fetch("exam", JoinType.INNER); // 또는 LEFT
                query.distinct(true); // 조인으로 중복 row 방지
            }

            List<Predicate> predicates = new ArrayList<>();
            if (criteria.getUserId() != null) {
                predicates.add(builder.equal(root.get("userId"), criteria.getUserId()));
            }
            // ✅ exam 필터는 join으로
            Join<ExamResult, Exam> exam = root.join("exam", JoinType.INNER);

            if (criteria.getExamYear() != null) {
                predicates.add(builder.equal(exam.get("examYear"), criteria.getExamYear()));
            }
            if (criteria.getExamType() != null) {
                predicates.add(builder.equal(exam.get("examType"), criteria.getExamType()));
            }
            if (criteria.getOpenerUsage() != null) {
                // 오프너분석 사용 여부
                if (criteria.getOpenerUsage()) {
                    // 오프너분석 사용
                    predicates.add(builder.greaterThanOrEqualTo(root.get("openerUsageCount"), 1));
                } else {
                    // 오프너분석 미사용
                    predicates.add(builder.equal(root.get("openerUsageCount"), 0));
                }
            }
            if (criteria.getCreatedAtFrom() != null ) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedAtFrom()));
            }
            if (criteria.getCreatedAtTo() != null ) {
                predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), criteria.getCreatedAtTo()));
            }
            if (criteria.getCategory() != null) {
                predicates.add(builder.equal(root.get("category"), criteria.getCategory()));
            }


            return predicates.isEmpty() ? builder.conjunction() : builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
