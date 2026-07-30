package spring.backend.domain.exam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.repository.spec.ExamRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
class ExamResultServiceImplTest {

    private static final Category CATEGORY = Category.ALG;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamResultRepository examResultRepository;

    @InjectMocks
    private ExamResultServiceImpl examResultService;

    @Test
    @DisplayName("startExam: userId 가 null 이면 USER_NOT_FOUND")
    void startExam_userIdNull_throw() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> examResultService.startExam(null, 1L, CATEGORY)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        // 조회 전에 끊는지 확인 -> 인자 검증이 뒤로 밀리면 불필요한 DB 왕복이 생김
        verifyNoInteractions(examRepository, examResultRepository);
    }

    @Test
    @DisplayName("startExam: examId 가 null 이면 USER_NOT_FOUND")
    void startExam_examIdNull_throw() {
        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> examResultService.startExam(UUID.randomUUID(), null, CATEGORY)
        );

        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(examRepository, examResultRepository);
    }

    @Test
    @DisplayName("startExam: 존재하지 않는 examId 이면 EXAM_NOT_FOUND")
    void startExam_invalidExamId_throw() {
        UUID userId = UUID.randomUUID();
        Long examId = 999L;

        when(examRepository.findById(examId)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> examResultService.startExam(userId, examId, CATEGORY)
        );

        assertEquals(ErrorCode.EXAM_NOT_FOUND, ex.getErrorCode());
        verify(examRepository).findById(examId);
        // 조회 실패 시 저장까지 가지 않는지 확인 -> 반쪽 응시 기록이 남으면 통계가 오염됨
        verifyNoInteractions(examResultRepository);
    }

    @Test
    @DisplayName("startExam: 정상 케이스 - ExamResult 저장 후 id 반환")
    void startExam_success_returnSavedId() {
        UUID userId = UUID.randomUUID();
        Long examId = 1L;
        Exam exam = mock(Exam.class);

        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        // id 는 JPA 가 채우는 값이라 테스트에서 직접 못 넣음 -> spy 로 반환값만 지정
        ExamResult savedSpy = spy(ExamResult.of(userId, exam));
        when(savedSpy.getId()).thenReturn(123L);
        when(examResultRepository.save(any(ExamResult.class))).thenReturn(savedSpy);

        Long resultId = examResultService.startExam(userId, examId, CATEGORY);

        assertEquals(123L, resultId);

        // 저장 직전 객체를 캡처 -> 반환값만 보면 엉뚱한 값으로 만들어 저장해도 통과함
        ArgumentCaptor<ExamResult> captor = ArgumentCaptor.forClass(ExamResult.class);
        verify(examResultRepository).save(captor.capture());
        ExamResult toSave = captor.getValue();

        assertEquals(userId, toSave.getUserId());
        assertEquals(exam, toSave.getExam());

        verify(examRepository).findById(examId);
    }
}
