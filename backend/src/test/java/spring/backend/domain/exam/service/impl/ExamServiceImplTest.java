package spring.backend.domain.exam.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.backend.domain.exam.dto.response.ExamResponse;
import spring.backend.domain.exam.mapper.ExamMapper;
import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.ExamType;
import spring.backend.domain.exam.repository.spec.ExamRepository;
import spring.backend.domain.exam.service.spec.ExamResultService;
import spring.backend.shared.response.exception.BusinessException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamResultService examResultService;

    @Mock
    private ExamMapper examMapper;

    @InjectMocks
    private ExamServiceImpl service;

    @Test
    @DisplayName("파라미터가 null이면 BusinessException 발생")
    void findExamWithQuestions_nullParams_throw() {
        UUID userId = UUID.randomUUID();

        // examYear null
        assertThrows(BusinessException.class,
                () -> service.findExamWithQuestions(userId, null, ExamType.CSAT, Category.GEO));
        // examType null
        assertThrows(BusinessException.class,
                () -> service.findExamWithQuestions(userId, 2021, null, Category.GEO));
        // category null
        assertThrows(BusinessException.class,
                () -> service.findExamWithQuestions(userId, 2021, ExamType.CSAT, null));

        // 어떤 저장소도 호출되지 않아야 함
        verifyNoInteractions(examRepository, examResultService, examMapper);
    }

    @Test
    @DisplayName("해당 연도의 시험이 없으면 BusinessException(EXAM_NOT_FOUND) 발생")
    void findExamWithQuestions_examNotFound_throw() {
        UUID userId = UUID.randomUUID();
        int examYear = 2026;
        ExamType examType = ExamType.CSAT;
        Category category = Category.PROB;

        when(examRepository.findByExamYearAndExamType(examYear, examType))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> service.findExamWithQuestions(userId, examYear, examType, category));

        verify(examRepository).findByExamYearAndExamType(examYear, examType);
        // 이후 다른 의존성은 호출되지 않아야 함
        verifyNoInteractions(examResultService, examMapper);
        verifyNoMoreInteractions(examRepository);
    }

    @Test
    @DisplayName("정상 흐름: Exam 및 Question 조회, ExamResult 시작, 매핑 후 ExamResponse 반환")
    void findExamWithQuestions_success() {
        UUID userId = UUID.randomUUID();
        int examYear = 2023;
        ExamType examType = ExamType.CSAT;
        Category category = Category.PROB;

        // mock Exam and its id
        Exam exam = mock(Exam.class);
        when(exam.getId()).thenReturn(100L);

        List<Question> questions = List.of(); // 빈 리스트로 테스트
        Long examResultId = 555L;
        ExamResponse expectedResponse = mock(ExamResponse.class);

        when(examRepository.findByExamYearAndExamType(examYear, examType))
                .thenReturn(Optional.of(exam));

        when(examRepository.findByExamIdAndCategoryInOrderByQuestionNoAsc(
                exam.getId(),
                List.of(Category.ALG, category)
        )).thenReturn(questions);

        when(examResultService.startExam(userId, exam.getId(), category)).thenReturn(examResultId);

        when(examMapper.toDto(exam, questions, examResultId)).thenReturn(expectedResponse);

        ExamResponse actual = service.findExamWithQuestions(userId, examYear, examType, category);

        assertSame(expectedResponse, actual);

        verify(examRepository).findByExamYearAndExamType(examYear, examType);
        verify(examRepository).findByExamIdAndCategoryInOrderByQuestionNoAsc(exam.getId(), List.of(Category.ALG, category));
        verify(examResultService).startExam(userId, exam.getId(), category);
        verify(examMapper).toDto(exam, questions, examResultId);
        verifyNoMoreInteractions(examRepository, examResultService, examMapper);
    }
}