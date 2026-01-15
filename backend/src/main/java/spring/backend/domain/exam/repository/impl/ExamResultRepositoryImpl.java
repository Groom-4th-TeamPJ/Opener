package spring.backend.domain.exam.repository.impl;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import spring.backend.domain.exam.model.dto.ExamResultSearchCriteria;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.repository.jpa.JpaExamResultRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        Specification<ExamResult> specification = toSpecification(criteria);
        return jpaExamResultRepository.findAll(specification, pageable);
    }

    private Specification<ExamResult> toSpecification(ExamResultSearchCriteria criteria) {
        return (root, query, builder) -> {
            if (criteria == null) {
                return builder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();
            if (criteria.getUserId() != null) {
                predicates.add(builder.equal(root.get("userId"), criteria.getUserId()));
            }
            if (criteria.getExamYear() != null) {
                predicates.add(builder.equal(root.get("examYear"), criteria.getExamYear()));
            }
            if (criteria.getExamType() != null) {
                predicates.add(builder.equal(root.get("examType"), criteria.getExamType()));
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


            return predicates.isEmpty() ? builder.conjunction() : builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
