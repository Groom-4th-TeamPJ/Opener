package spring.backend.domain.exam.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.exam.dto.response.ExamResponse;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.ExamType;
import spring.backend.domain.exam.repository.spec.ExamRepository;
import spring.backend.domain.exam.service.spec.ExamService;

import java.util.Optional;

@Service
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;

    public ExamServiceImpl(ExamRepository examRepository) {
        this.examRepository = examRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<ExamResponse> findExamWithQuestions(Integer examYear, ExamType examType, Category category) {

        // TODO :: 캔 차감
        return examRepository.findExamWithQuestions(examYear, examType, category);
    }
}
