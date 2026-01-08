package spring.backend.domain.exam.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.exam.dto.request.SubmitAnswerRequest;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.repository.spec.ExamRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.domain.exam.service.spec.ExamResultService;

import java.util.UUID;

@Service
public class ExamResultServiceImpl implements ExamResultService {

    private final ExamResultRepository examResultRepository;
    private final QuestionResultRepository questionResultRepository;
    private final ExamRepository examRepository;

    public ExamResultServiceImpl(ExamResultRepository examResultRepository, QuestionResultRepository questionResultRepository, ExamRepository examRepository) {
        this.examResultRepository = examResultRepository;
        this.questionResultRepository = questionResultRepository;
        this.examRepository = examRepository;
    }

    @Transactional
    @Override
    public Long startExam(UUID userId, Long examId) {
        if (userId == null || examId == null) {
            throw new IllegalArgumentException("userId and examId must not be null");
        }

        if (!examRepository.existsById(examId)) {
            throw new IllegalArgumentException("Invalid examId: " + examId);
        }

        ExamResult examResult = ExamResult.of(userId ,examId);
        ExamResult saved = examResultRepository.save(examResult);

        return saved.getId();
    }

    @Override
    public Long submitAnswers(Long examResultId, Long questionId, Long userId, SubmitAnswerRequest request) {
        return 0L;
    }
}
