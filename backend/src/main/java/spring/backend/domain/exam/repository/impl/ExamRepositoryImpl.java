package spring.backend.domain.exam.repository.impl;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.exam.dto.response.ExamResponse;
import spring.backend.domain.exam.mapper.ExamMapper;
import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.ExamType;
import spring.backend.domain.exam.repository.jpa.JpaExamRepository;
import spring.backend.domain.exam.repository.jpa.JpaQuestionRepository;
import spring.backend.domain.exam.repository.spec.ExamRepository;

import java.util.*;

@Repository
public class ExamRepositoryImpl implements ExamRepository {

    private final JpaExamRepository jpaExamRepository;
    private final JpaQuestionRepository jpaQuestionRepository;
    private final ExamMapper examMapper;

    public ExamRepositoryImpl(JpaExamRepository jpaExamRepository, JpaQuestionRepository jpaQuestionRepository, ExamMapper examMapper) {
        this.jpaExamRepository = jpaExamRepository;
        this.jpaQuestionRepository = jpaQuestionRepository;
        this.examMapper = examMapper;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<ExamResponse> findExamWithQuestions(Integer examYear, ExamType examType, Category category) {
        Exam exam = jpaExamRepository.findByExamYearAndExamType(examYear, examType)
                .orElseThrow(() ->
                        new IllegalArgumentException("Exam not found for year: " + examYear + " and examType: " + examType));

        if (exam == null) return Optional.empty();

        List<Category> categories = new ArrayList<>();
        categories.add(Category.ALG);

        if (Objects.nonNull(category) && category != Category.ALG) {
            categories.add(category);
        }

        List<Question> questions = jpaQuestionRepository.findByExamIdAndCategoryInOrderByQuestionNoAsc(exam.getId(), categories);

        return Optional.of(examMapper.toDto(exam, questions));
    }
}
