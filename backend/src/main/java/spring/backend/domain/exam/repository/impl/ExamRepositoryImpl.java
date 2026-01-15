package spring.backend.domain.exam.repository.impl;

import org.springframework.stereotype.Repository;
import spring.backend.domain.exam.mapper.ExamMapper;
import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.ExamType;
import spring.backend.domain.exam.repository.jpa.JpaExamRepository;
import spring.backend.domain.exam.repository.jpa.JpaQuestionRepository;
import spring.backend.domain.exam.repository.spec.ExamRepository;

import java.util.List;
import java.util.Optional;

@Repository
public class ExamRepositoryImpl implements ExamRepository {

    private final JpaExamRepository jpaExamRepository;
    private final JpaQuestionRepository jpaQuestionRepository;

    public ExamRepositoryImpl(JpaExamRepository jpaExamRepository, JpaQuestionRepository jpaQuestionRepository) {
        this.jpaExamRepository = jpaExamRepository;
        this.jpaQuestionRepository = jpaQuestionRepository;
    }

    @Override
    public List<Question> findByExamIdAndCategoryInOrderByQuestionNoAsc(Long examId, List<Category> categories) {
        return jpaQuestionRepository.findByExamIdAndCategoryInOrderByQuestionNoAsc(examId, categories);
    }

    @Override
    public Optional<Exam> findById(Long id) {
        return jpaExamRepository.findById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaExamRepository.existsById(id);
    }

    @Override
    public Optional<Question> findQuestionById(Long id) {
        return jpaQuestionRepository.findById(id);
    }

    @Override
    public Optional<Exam> findByExamYearAndExamType(Integer examYear, ExamType examType) {
        return jpaExamRepository.findByExamYearAndExamType(examYear, examType);
    }
}
