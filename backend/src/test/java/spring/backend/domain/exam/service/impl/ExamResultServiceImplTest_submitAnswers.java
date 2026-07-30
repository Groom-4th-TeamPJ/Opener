package spring.backend.domain.exam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.backend.domain.exam.dto.request.SubmitAnswerRequest;
import spring.backend.domain.exam.dto.response.SubmitAnswerResponse;
import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.exam.repository.spec.ExamRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ExamResultServiceImplTest_submitAnswers {

    private static final Long EXAM_ID = 1L;
    private static final Long EXAM_RESULT_ID = 10L;
    private static final Long QUESTION_ID = 5L;

    @Mock
    ExamResultRepository examResultRepository;
    @Mock
    QuestionResultRepository questionResultRepository;
    @Mock
    ExamRepository examRepository;

    @InjectMocks
    ExamResultServiceImpl service;

    private SubmitAnswerRequest req(Integer selected, Integer timeSpent) {
        return SubmitAnswerRequest.builder()
                .selected(selected)
                .timeSpent(timeSpent)
                .build();
    }

    // Question 은 정적 팩토리가 없어 mock 으로만 조립 가능 -> exam 소속까지 stub 해야 QUESTION_NOT_IN_EXAM 을 안 탐
    private Question questionOf(Exam exam, Integer answer) {
        Question question = mock(Question.class);
        when(question.getExam()).thenReturn(exam);
        when(question.getAnswer()).thenReturn(answer);
        return question;
    }

    @Test
    @DisplayName("submitAnswers: 파라미터가 null 이면 MISSING_PARAMETER")
    void submitAnswers_nullParams_throw() {
        UUID userId = UUID.randomUUID();
        SubmitAnswerRequest request = req(3, 10);

        assertEquals(ErrorCode.MISSING_PARAMETER, assertThrows(BusinessException.class,
                () -> service.submitAnswers(null, QUESTION_ID, userId, request)).getErrorCode());
        assertEquals(ErrorCode.MISSING_PARAMETER, assertThrows(BusinessException.class,
                () -> service.submitAnswers(EXAM_RESULT_ID, null, userId, request)).getErrorCode());
        assertEquals(ErrorCode.MISSING_PARAMETER, assertThrows(BusinessException.class,
                () -> service.submitAnswers(EXAM_RESULT_ID, QUESTION_ID, null, request)).getErrorCode());
        assertEquals(ErrorCode.MISSING_PARAMETER, assertThrows(BusinessException.class,
                () -> service.submitAnswers(EXAM_RESULT_ID, QUESTION_ID, userId, null)).getErrorCode());
        // 요청 본문 필드까지 같은 코드로 막는지 확인 -> 여기서 새면 QuestionResult.of 의 언박싱에서 NPE 가 남
        assertEquals(ErrorCode.MISSING_PARAMETER, assertThrows(BusinessException.class,
                () -> service.submitAnswers(EXAM_RESULT_ID, QUESTION_ID, userId, req(null, 10))).getErrorCode());
        assertEquals(ErrorCode.MISSING_PARAMETER, assertThrows(BusinessException.class,
                () -> service.submitAnswers(EXAM_RESULT_ID, QUESTION_ID, userId, req(3, null))).getErrorCode());

        verifyNoInteractions(examResultRepository, questionResultRepository, examRepository);
    }

    @Test
    @DisplayName("submitAnswers: 이미 제출된 문제면 RESULT_ALREADY_SUBMITTED + 이후 조회/저장 호출 없음")
    void submitAnswers_alreadySubmitted_throw() {
        UUID userId = UUID.randomUUID();

        // Given: 해당 examResultId, questionId 조합이 이미 제출된 상태
        when(questionResultRepository.existsByExamResultIdAndQuestionId(EXAM_RESULT_ID, QUESTION_ID))
                .thenReturn(true);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.submitAnswers(EXAM_RESULT_ID, QUESTION_ID, userId, req(3, 12))
        );

        assertEquals(ErrorCode.RESULT_ALREADY_SUBMITTED, ex.getErrorCode());

        // 중복 체크에서 바로 끊는지 확인 -> 통과하면 유니크 제약 위반이 500 으로 새어 나감
        verify(questionResultRepository).existsByExamResultIdAndQuestionId(EXAM_RESULT_ID, QUESTION_ID);
        verifyNoMoreInteractions(questionResultRepository);
        verifyNoInteractions(examResultRepository, examRepository);
    }

    @Test
    @DisplayName("submitAnswers: ExamResult 를 찾지 못하면 RESULT_NOT_FOUND")
    void submitAnswers_examResultNotFound_throw() {
        UUID userId = UUID.randomUUID();

        when(questionResultRepository.existsByExamResultIdAndQuestionId(EXAM_RESULT_ID, QUESTION_ID))
                .thenReturn(false);
        // userId 를 함께 넘겨 조회 -> 남의 응시 기록에 답을 꽂지 못하게 막는 지점
        when(examResultRepository.findByIdAndUserId(EXAM_RESULT_ID, userId))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.submitAnswers(EXAM_RESULT_ID, QUESTION_ID, userId, req(3, 12))
        );

        assertEquals(ErrorCode.RESULT_NOT_FOUND, ex.getErrorCode());

        verify(questionResultRepository).existsByExamResultIdAndQuestionId(EXAM_RESULT_ID, QUESTION_ID);
        verify(examResultRepository).findByIdAndUserId(EXAM_RESULT_ID, userId);
        verifyNoInteractions(examRepository);
        verify(questionResultRepository, never()).save(any());
        verify(examResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitAnswers: Question 을 찾지 못하면 QUESTION_NOT_FOUND")
    void submitAnswers_questionNotFound_throw() {
        UUID userId = UUID.randomUUID();

        when(questionResultRepository.existsByExamResultIdAndQuestionId(EXAM_RESULT_ID, QUESTION_ID))
                .thenReturn(false);

        ExamResult examResult = mock(ExamResult.class);
        when(examResultRepository.findByIdAndUserId(EXAM_RESULT_ID, userId))
                .thenReturn(Optional.of(examResult));

        when(examRepository.findQuestionById(QUESTION_ID))
                .thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.submitAnswers(EXAM_RESULT_ID, QUESTION_ID, userId, req(3, 12))
        );

        assertEquals(ErrorCode.QUESTION_NOT_FOUND, ex.getErrorCode());

        verify(questionResultRepository).existsByExamResultIdAndQuestionId(EXAM_RESULT_ID, QUESTION_ID);
        verify(examResultRepository).findByIdAndUserId(EXAM_RESULT_ID, userId);
        verify(examRepository).findQuestionById(QUESTION_ID);

        verify(questionResultRepository, never()).save(any());
        verify(examResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitAnswers: 다른 시험의 문제를 제출하면 QUESTION_NOT_IN_EXAM + 저장 없음")
    void submitAnswers_questionFromOtherExam_throw() {
        UUID userId = UUID.randomUUID();

        Exam exam = mock(Exam.class);
        when(exam.getId()).thenReturn(EXAM_ID);
        Exam otherExam = mock(Exam.class);
        when(otherExam.getId()).thenReturn(99L);

        when(questionResultRepository.existsByExamResultIdAndQuestionId(EXAM_RESULT_ID, QUESTION_ID))
                .thenReturn(false);

        ExamResult examResult = ExamResult.of(userId, exam);
        when(examResultRepository.findByIdAndUserId(EXAM_RESULT_ID, userId))
                .thenReturn(Optional.of(examResult));

        Question question = mock(Question.class);
        when(question.getExam()).thenReturn(otherExam);
        when(examRepository.findQuestionById(QUESTION_ID))
                .thenReturn(Optional.of(question));

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> service.submitAnswers(EXAM_RESULT_ID, QUESTION_ID, userId, req(3, 12))
        );

        assertEquals(ErrorCode.QUESTION_NOT_IN_EXAM, ex.getErrorCode());

        // 남의 시험 문제로 점수가 붙지 않는지 확인
        assertEquals(0, examResult.getTotalScore());
        verify(questionResultRepository, never()).save(any());
        verify(examResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitAnswers: 정답이면 QuestionResult 저장 + score/time/정답수 누적 + ExamResult 저장 + 응답 반환")
    void submitAnswers_correct_flow() {
        UUID userId = UUID.randomUUID();
        SubmitAnswerRequest request = req(3, 10);

        Exam exam = mock(Exam.class);
        when(exam.getId()).thenReturn(EXAM_ID);

        when(questionResultRepository.existsByExamResultIdAndQuestionId(EXAM_RESULT_ID, QUESTION_ID))
                .thenReturn(false);

        // 누적 결과를 실제 게터로 보려고 진짜 엔티티 사용 -> mock 이면 addScore 호출만 보고 계산 오류를 놓침
        ExamResult examResult = ExamResult.of(userId, exam);
        when(examResultRepository.findByIdAndUserId(EXAM_RESULT_ID, userId))
                .thenReturn(Optional.of(examResult));

        Question question = questionOf(exam, 3);
        when(question.getPoint()).thenReturn(2);
        when(examRepository.findQuestionById(QUESTION_ID))
                .thenReturn(Optional.of(question));

        // id 는 JPA 가 채우는 값이라 저장 반환값만 mock 으로 지정
        QuestionResult savedQr = mock(QuestionResult.class);
        when(savedQr.getId()).thenReturn(100L);
        when(questionResultRepository.save(any(QuestionResult.class))).thenReturn(savedQr);

        SubmitAnswerResponse response = service.submitAnswers(EXAM_RESULT_ID, QUESTION_ID, userId, request);

        assertEquals(100L, response.getQuestionResultId());
        assertTrue(response.isCorrect());
        assertEquals(3, response.getAnswer());

        assertEquals(2, examResult.getTotalScore());
        assertEquals(10, examResult.getTotalTimeSpent());
        assertEquals(1, examResult.getCorrectCount());
        assertEquals(0, examResult.getIncorrectCount());

        // 저장 직전 객체를 캡처 -> 반환값만 보면 채점 결과를 안 찍고 저장해도 통과함
        ArgumentCaptor<QuestionResult> captor = ArgumentCaptor.forClass(QuestionResult.class);
        verify(questionResultRepository).save(captor.capture());
        QuestionResult toSave = captor.getValue();

        assertTrue(toSave.isCorrect());
        assertEquals(3, toSave.getSelected());
        assertEquals(10, toSave.getTimeSpent());
        assertEquals(examResult, toSave.getExamResult());
        assertEquals(question, toSave.getQuestion());

        verify(examResultRepository).save(examResult);
    }

    @Test
    @DisplayName("submitAnswers: 오답이면 score 누적 없음 + time/오답수 누적 + 저장 + 응답 반환")
    void submitAnswers_incorrect_flow() {
        UUID userId = UUID.randomUUID();
        SubmitAnswerRequest request = req(2, 20);

        Exam exam = mock(Exam.class);
        when(exam.getId()).thenReturn(EXAM_ID);

        when(questionResultRepository.existsByExamResultIdAndQuestionId(EXAM_RESULT_ID, QUESTION_ID))
                .thenReturn(false);

        ExamResult examResult = ExamResult.of(userId, exam);
        when(examResultRepository.findByIdAndUserId(EXAM_RESULT_ID, userId))
                .thenReturn(Optional.of(examResult));

        // 정답 3, 제출 2 -> 오답
        Question question = questionOf(exam, 3);
        when(examRepository.findQuestionById(QUESTION_ID))
                .thenReturn(Optional.of(question));

        QuestionResult savedQr = mock(QuestionResult.class);
        when(savedQr.getId()).thenReturn(200L);
        when(questionResultRepository.save(any(QuestionResult.class))).thenReturn(savedQr);

        SubmitAnswerResponse response = service.submitAnswers(EXAM_RESULT_ID, QUESTION_ID, userId, request);

        assertEquals(200L, response.getQuestionResultId());
        assertFalse(response.isCorrect());
        assertEquals(3, response.getAnswer());

        // 오답에 점수가 붙지 않는지 확인 -> 배점 조회 자체가 없어야 함
        assertEquals(0, examResult.getTotalScore());
        verify(question, never()).getPoint();
        assertEquals(20, examResult.getTotalTimeSpent());
        assertEquals(0, examResult.getCorrectCount());
        assertEquals(1, examResult.getIncorrectCount());

        ArgumentCaptor<QuestionResult> captor = ArgumentCaptor.forClass(QuestionResult.class);
        verify(questionResultRepository).save(captor.capture());
        assertFalse(captor.getValue().isCorrect());
        assertEquals(2, captor.getValue().getSelected());

        verify(examResultRepository).save(examResult);
    }
}
