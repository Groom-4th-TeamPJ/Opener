package spring.backend.domain.exam.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.backend.domain.exam.dto.request.SubmitAnswerRequest;
import spring.backend.domain.exam.dto.response.SubmitAnswerResponse;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.exam.repository.spec.ExamRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamResultServiceImplTest_submitAnswers {

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

    @Test
    @DisplayName("파라미터가 null이면 IllegalArgumentException 발생")
    void submitAnswers_nullParams_throw() {
        UUID userId = UUID.randomUUID();
        SubmitAnswerRequest req = req(3, 10);

        assertThrows(IllegalArgumentException.class,
                () -> service.submitAnswers(null, 1L, userId, req));
        assertThrows(IllegalArgumentException.class,
                () -> service.submitAnswers(1L, null, userId, req));
        assertThrows(IllegalArgumentException.class,
                () -> service.submitAnswers(1L, 1L, null, req));
        assertThrows(IllegalArgumentException.class,
                () -> service.submitAnswers(1L, 1L, userId, null));

        verifyNoInteractions(examResultRepository, questionResultRepository, examRepository);
    }

    @Test
    @DisplayName("이미 제출된 문제면(중복 제출) IllegalArgumentException 발생 + 이후 조회/저장 호출 없음")
    void submitAnswers_alreadySubmitted_throw() {
        Long examResultId = 10L;
        Long questionId = 5L;
        UUID userId = UUID.randomUUID();

        // Given: 특정 examResultId, questionId에 대해 이미 제출된 상태를 시뮬레이션
        when(questionResultRepository.existsByExamResultIdAndQuestionId(examResultId, questionId))
                .thenReturn(true);

        // When: submitAnswers를 호출하면
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.submitAnswers(examResultId, questionId, userId, req(3, 12))
        );

        // Then: 예외 메시지에 'already been submitted'가 포함되어야 한다
        assertTrue(ex.getMessage().contains("already been submitted"));

        // 그리고 questionResultRepository의 중복 체크만 호출되었는지 확인
        verify(questionResultRepository).existsByExamResultIdAndQuestionId(examResultId, questionId);
        verifyNoMoreInteractions(questionResultRepository);
        // 다른 저장소(examResultRepository, examRepository)는 전혀 호출되지 않아야 함
        verifyNoInteractions(examResultRepository, examRepository);
    }

    @Test
    @DisplayName("ExamResult를 찾지 못하면 IllegalArgumentException 발생")
    void submitAnswers_examResultNotFound_throw() {
        Long examResultId = 10L;
        Long questionId = 5L;
        UUID userId = UUID.randomUUID();

        when(questionResultRepository.existsByExamResultIdAndQuestionId(examResultId, questionId))
                .thenReturn(false);

        when(examResultRepository.findByIdAndUserId(examResultId, userId))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.submitAnswers(examResultId, questionId, userId, req(3, 12))
        );

        assertTrue(ex.getMessage().contains("ExamResult not found"));

        verify(questionResultRepository).existsByExamResultIdAndQuestionId(examResultId, questionId);
        verify(examResultRepository).findByIdAndUserId(examResultId, userId);
        verifyNoInteractions(examRepository);
        verify(questionResultRepository, never()).save(any());
        verify(examResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("Question을 찾지 못하면 IllegalArgumentExceptio")
    void submitAnswers_questionNotFound_throw() {
        Long examResultId = 10L;
        Long questionId = 5L;
        UUID userId = UUID.randomUUID();

        when(questionResultRepository.existsByExamResultIdAndQuestionId(examResultId, questionId))
                .thenReturn(false);

        ExamResult examResult = mock(ExamResult.class);
        when(examResultRepository.findByIdAndUserId(examResultId, userId))
                .thenReturn(Optional.of(examResult));

        when(examRepository.findQuestionById(questionId))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.submitAnswers(examResultId, questionId, userId, req(3, 12))
        );

        assertTrue(ex.getMessage().contains("Question not found"));

        verify(questionResultRepository).existsByExamResultIdAndQuestionId(examResultId, questionId);
        verify(examResultRepository).findByIdAndUserId(examResultId, userId);
        verify(examRepository).findQuestionById(questionId);

        verify(questionResultRepository, never()).save(any());
        verify(examResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("정답이면: QuestionResult 저장 + ExamResult score/time 누적 + ExamResult 저장 + 응답 반환")
    void submitAnswers_correct_flow() {
        Long examResultId = 10L;
        Long questionId = 5L;
        UUID userId = UUID.randomUUID();

        SubmitAnswerRequest req = req(3, 10);

        when(questionResultRepository.existsByExamResultIdAndQuestionId(examResultId, questionId))
                .thenReturn(false);

        ExamResult examResult = mock(ExamResult.class);
        when(examResultRepository.findByIdAndUserId(examResultId, userId))
                .thenReturn(Optional.of(examResult));

        Question question = mock(Question.class);
        when(examRepository.findQuestionById(questionId))
                .thenReturn(Optional.of(question));

        ExamResultServiceImpl spyService = spy(service);
        doReturn(true).when(spyService).isAnswerCorrect(question, req.getSelected());

        when(question.getPoint()).thenReturn(2);

        QuestionResult savedQr = mock(QuestionResult.class);
        when(savedQr.getId()).thenReturn(100L);
        when(questionResultRepository.save(any(QuestionResult.class)))
                .thenReturn(savedQr);

        SubmitAnswerResponse response = spyService.submitAnswers(examResultId, questionId, userId, req);

        assertEquals(100L, response.getQuestionResultId());
        assertTrue(response.isCorrect());

        verify(examResult).addScore(2);
        verify(examResult).addTimeSpent(10);

        verify(questionResultRepository).save(any(QuestionResult.class));
        verify(examResultRepository).save(examResult);
    }

    @Test
    @DisplayName("오답이면: score 누적 없음 + time 누적 + 저장 + 응답 반환")
    void submitAnswers_incorrect_flow() {
        Long examResultId = 10L;
        Long questionId = 5L;
        UUID userId = UUID.randomUUID();

        SubmitAnswerRequest request = req(2, 20);

        when(questionResultRepository.existsByExamResultIdAndQuestionId(examResultId, questionId))
                .thenReturn(false);

        ExamResult examResult = mock(ExamResult.class);
        when(examResultRepository.findByIdAndUserId(examResultId, userId))
                .thenReturn(Optional.of(examResult));

        Question question = mock(Question.class);
        when(examRepository.findQuestionById(questionId))
                .thenReturn(Optional.of(question));

        ExamResultServiceImpl spyService = spy(service);
        doReturn(false).when(spyService).isAnswerCorrect(question, request.getSelected());

        QuestionResult savedQr = mock(QuestionResult.class);
        when(savedQr.getId()).thenReturn(200L);
        when(questionResultRepository.save(any(QuestionResult.class))).thenReturn(savedQr);

        SubmitAnswerResponse response = spyService.submitAnswers(examResultId, questionId, userId, request);

        assertEquals(200L, response.getQuestionResultId());
        assertFalse(response.isCorrect());

        verify(examResult, never()).addScore(anyInt());
        verify(examResult).addTimeSpent(20);

        verify(questionResultRepository).save(any(QuestionResult.class));
        verify(examResultRepository).save(examResult);
    }


}