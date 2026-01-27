package spring.backend.domain.exam.service.spec;

import spring.backend.domain.exam.dto.request.SubmitAnswerRequest;
import spring.backend.domain.exam.dto.response.ExamResultSummaryResponse;
import spring.backend.domain.exam.dto.response.SubmitAnswerResponse;
import spring.backend.domain.exam.model.enums.Category;

import java.util.UUID;

public interface ExamResultService {

    Long startExam(UUID userId, Long examId, Category category);

    SubmitAnswerResponse submitAnswers(Long examResultId, Long questionId, UUID userId, SubmitAnswerRequest request);

    ExamResultSummaryResponse getExamResultSummary(Long examResultId, UUID userId);
}
