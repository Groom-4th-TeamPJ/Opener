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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
    public Optional<ExamResponse> findExamWithQuestions(Integer year, ExamType examType, Category category) {
        Exam exam = jpaExamRepository.findByYearAndExamType(year, examType)
                .orElseThrow(() ->
                        new IllegalArgumentException("Exam not found for year: " + year + " and examType: " + examType));

        if (exam == null) return Optional.empty();

        List<Question> questions = jpaQuestionRepository.findByExamIdAndCategoryOrderByOrderAsc(exam.getId(), category);

        return Optional.of(examMapper.toDto(exam, questions == null ? Collections.emptyList() : questions));
    }
}
