package spring.backend.domain.exam.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.exam.dto.response.ExamResponse;
import spring.backend.domain.exam.mapper.ExamMapper;
import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.ExamType;
import spring.backend.domain.exam.repository.spec.ExamRepository;
import spring.backend.domain.exam.service.spec.ExamResultService;
import spring.backend.domain.exam.service.spec.ExamService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamResultService examResultService;
    private final ExamMapper examMapper;

    public ExamServiceImpl(ExamRepository examRepository, ExamResultService examResultService, ExamMapper examMapper) {
        this.examRepository = examRepository;
        this.examResultService = examResultService;
        this.examMapper = examMapper;
    }

    @Transactional
    @Override
    public ExamResponse findExamWithQuestions(UUID userId, Integer examYear, ExamType examType, Category category) {
        log.info("Finding exam for year: {}, type: {}, category: {}", examYear, examType, category);

        if (examYear == null || examType == null || category == null) {
            throw new BusinessException(ErrorCode.MISSING_PARAMETER);
        }

        Exam exam = examRepository.findByExamYearAndExamType(examYear, examType)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));

        List<Question> questions = examRepository.findByExamIdAndCategoryInOrderByQuestionNoAsc(
                exam.getId(),
                List.of(Category.ALG, category)
        );

        // ExamResult 시작
        Long examResultId = startExamResult(userId, exam.getId(), category);

        ExamResponse response = examMapper.toDto(exam, questions, examResultId);

        if (response == null) {
            throw new BusinessException(ErrorCode.EXAM_NOT_FOUND);
        }

        return response;
    }

    private Long startExamResult(UUID userId, Long examId, Category category) {
        return examResultService.startExam(userId, examId, category);
    }
}
