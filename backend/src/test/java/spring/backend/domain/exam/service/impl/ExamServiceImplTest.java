package spring.backend.domain.exam.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.backend.domain.exam.dto.response.ExamResponse;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.ExamType;
import spring.backend.domain.exam.repository.spec.ExamRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {

    @Mock
    private ExamRepository examRepository;

    private ExamServiceImpl examService;

    @BeforeEach
    void setUp() {
        examService = new ExamServiceImpl(examRepository);
    }

    @Test
    void findExamWithQuestions_returnsPresentWhenRepositoryHasData() {
        Integer examYear = 2026;
        ExamType examType = ExamType.CSAT;
        Category category = Category.GEO;

        ExamResponse stubResponse = ExamResponse.builder()
                .exam(null)
                .questions(List.of())
                .build();

        when(examRepository.findExamWithQuestions(examYear, examType, category))
                .thenReturn(Optional.of(stubResponse));

        Optional<ExamResponse> result = examService.findExamWithQuestions(examYear, examType, category);

        assertTrue(result.isPresent());
        assertSame(stubResponse, result.get());
        verify(examRepository, times(1)).findExamWithQuestions(examYear, examType, category);
        verifyNoMoreInteractions(examRepository);
    }

    @Test
    void findExamWithQuestions_returnsEmptyWhenRepositoryHasNoData() {
        Integer examYear = 2025;
        ExamType examType = ExamType.CSAT;
        Category category = Category.GEO;

        when(examRepository.findExamWithQuestions(examYear, examType, category))
                .thenReturn(Optional.empty());

        Optional<ExamResponse> result = examService.findExamWithQuestions(examYear, examType, category);

        assertFalse(result.isPresent());
        verify(examRepository, times(1)).findExamWithQuestions(examYear, examType, category);
        verifyNoMoreInteractions(examRepository);
    }

}