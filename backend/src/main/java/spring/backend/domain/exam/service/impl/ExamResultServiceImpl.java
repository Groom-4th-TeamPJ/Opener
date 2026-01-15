package spring.backend.domain.exam.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.exam.dto.request.SubmitAnswerRequest;
import spring.backend.domain.exam.dto.response.SubmitAnswerResponse;
import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.exam.repository.spec.ExamRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.domain.exam.service.spec.ExamResultService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

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
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));

        ExamResult examResult = ExamResult.of(userId, exam);
        ExamResult saved = examResultRepository.save(examResult);

        return saved.getId();
    }

    @Transactional
    @Override
    public SubmitAnswerResponse submitAnswers(Long examResultId, Long questionId, UUID userId, SubmitAnswerRequest request) {
        // 파라미터 검증
        if (examResultId == null || questionId == null || userId == null || request == null
                    || request.getSelected() == null || request.getTimeSpent() == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER);
        }

        // QuestionResult에 이미 제출된 답안 검증
        if (questionResultRepository.existsByExamResultIdAndQuestionId(examResultId, questionId)) {
            throw new BusinessException(ErrorCode.RESULT_ALREADY_SUBMITTED);
        }

        ExamResult examResult = examResultRepository.findByIdAndUserId(examResultId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESULT_NOT_FOUND));

        Question question = examRepository.findQuestionById(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // 질문이 해당 시험에 속하는지 검증
        if (question.getExam() == null || question.getExam().getId() == null || !question.getExam().getId().equals(examResult.getExam().getId())) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_IN_EXAM);
        }

        // QuestionResult 엔티티 생성
        QuestionResult questionResult = QuestionResult.of(examResult, question, request.getSelected(), request.getTimeSpent());
        boolean isCorrect = isAnswerCorrect(question, request.getSelected()); // 정답 여부 체크
        questionResult.markCorrect(isCorrect);

        QuestionResult saved = questionResultRepository.save(questionResult); // QuestionResult 저장

        if (isCorrect) {
            Integer point = question.getPoint();
            if (point != null) {
                examResult.addScore(question.getPoint());    // ExamResult 점수 갱신
            }
        }
        examResult.addTimeSpent(request.getTimeSpent()); // ExamResult 소요 시간 갱신
        examResultRepository.save(examResult);           // ExamResult 저장

        return new SubmitAnswerResponse(saved.getId(), isCorrect, question.getAnswer());
    }

    // 정답 확인 메서드
    protected boolean isAnswerCorrect(Question question, Integer selectedAnswer) {
        if (selectedAnswer == null || question.getAnswer() == null) {
            return false;
        }

        return question.getAnswer().equals(selectedAnswer);
    }
}
