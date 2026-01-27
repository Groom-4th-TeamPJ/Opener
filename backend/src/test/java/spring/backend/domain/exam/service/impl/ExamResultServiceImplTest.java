package spring.backend.domain.exam.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.repository.spec.ExamRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamResultServiceImplTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamResultRepository examResultRepository;

    @InjectMocks
    private ExamResultServiceImpl examResultService; // 네 구현 클래스 이름에 맞춰 수정

    @Test
    @DisplayName("startExam: userId가 null이면 IllegalArgumentException")
    void startExam_userIdNull_throw() {
        Long examId = 1L;

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> examResultService.startExam(null, examId)
        );

        assertTrue(ex.getMessage().contains("must not be null"));
        verifyNoInteractions(examRepository, examResultRepository);
    }

    @Test
    @DisplayName("startExam: examId가 null이면 IllegalArgumentException")
    void startExam_examIdNull_throw() {
        UUID userId = UUID.randomUUID();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> examResultService.startExam(userId, null)
        );

        assertTrue(ex.getMessage().contains("must not be null"));
        verifyNoInteractions(examRepository, examResultRepository);
    }

    @Test
    @DisplayName("startExam: 존재하지 않는 examId이면 IllegalArgumentException")
    void startExam_invalidExamId_throw() {
        UUID userId = UUID.randomUUID();
        Long examId = 999L;

        when(examRepository.existsById(examId)).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> examResultService.startExam(userId, examId)
        );

        assertTrue(ex.getMessage().contains("Invalid examId"));
        verify(examRepository).existsById(examId);
        verifyNoInteractions(examResultRepository);
    }

    @Test
    @DisplayName("startExam: 정상 케이스 - ExamResult 저장 후 id 반환")
    void startExam_success_returnSavedId() {
        UUID userId = UUID.randomUUID();
        Long examId = 1L;

        when(examRepository.existsById(examId)).thenReturn(true);

        // save()가 반환할 엔티티 준비 (id가 있어야 함)
        ExamResult saved = ExamResult.of(userId, examId);
        // id는 private + setter 없음일 가능성이 높으니 Mock으로 처리하는 게 안전
        ExamResult savedSpy = spy(saved);
        when(savedSpy.getId()).thenReturn(123L);

        // save 인자로 들어오는 객체는 어떤 것이든 받아서 savedSpy 반환
        when(examResultRepository.save(any(ExamResult.class))).thenReturn(savedSpy);

        Long resultId = examResultService.startExam(userId, examId);

        assertEquals(123L, resultId);

        // save에 들어간 객체가 userId/examId를 갖고 있는지 확인(캡처)
        ArgumentCaptor<ExamResult> captor = ArgumentCaptor.forClass(ExamResult.class);
        verify(examResultRepository).save(captor.capture());
        ExamResult toSave = captor.getValue();
        assertEquals(userId, toSave.getUserId());
        assertEquals(examId, toSave.getExamId());

        verify(examRepository).existsById(examId);
    }
}