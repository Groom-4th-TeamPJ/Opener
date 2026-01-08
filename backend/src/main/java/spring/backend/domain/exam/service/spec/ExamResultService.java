package spring.backend.domain.exam.service.spec;

import spring.backend.domain.exam.dto.request.SubmitAnswerRequest;

import java.util.UUID;

public interface ExamResultService {

    Long startExam(UUID userId, Long examId);

    Long submitAnswers(Long examResultId, Long questionId, Long userId, SubmitAnswerRequest request);
}
