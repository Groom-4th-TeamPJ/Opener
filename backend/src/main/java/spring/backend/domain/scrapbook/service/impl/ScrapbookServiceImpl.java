package spring.backend.domain.scrapbook.service.impl;



import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.exam.repository.spec.ExamRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.domain.scrapbook.dto.response.ScrapbookFilterResponse;
import spring.backend.domain.scrapbook.dto.response.ScrapbookQuestionResult;
import spring.backend.domain.scrapbook.dto.response.ScrapbookResponse;
import spring.backend.domain.scrapbook.mapper.ScrapbookMapper;
import spring.backend.domain.scrapbook.service.spec.ScrapbookService;
import spring.backend.shared.response.PageResponse;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ScrapbookServiceImpl implements ScrapbookService {

    private final ExamResultRepository examResultRepository;
    private final ExamRepository examRepository;
    private final QuestionResultRepository questionResultRepository;

    public ScrapbookServiceImpl(ExamResultRepository examResultRepository,
                                ExamRepository examRepository,
                                QuestionResultRepository questionResultRepository) {
        this.examResultRepository = examResultRepository;
        this.examRepository = examRepository;
        this.questionResultRepository = questionResultRepository;
    }

    @Override
    public List<ScrapbookFilterResponse> getScrapbookFilters(UUID userId) {
        return examResultRepository.findScrapbookFiltersByUserId(userId);
    }

    /**
     * 사용자가 선택한 스크랩 분류의 모든 문제 추출
     *
     * @param userId
     * @param examId
     * @param pageable
     * @return
     */
    @Transactional(readOnly = true)
    @Override
    public ScrapbookResponse getScrapbookContents(UUID userId, Long examId, Pageable pageable) {
        log.info("스크랩북 분류 선택 후 리스트 조회 - userId: {}, examId: {}, pageable: {}", userId, examId, pageable);

        // 1. Exam ID로 시험 정보 조회
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXAM_NOT_FOUND));

        // 2. 해당 시험에 대한 List<ExamResult> LastOpenerUsageDate가 Not Null 조회
        List<ExamResult> examResults = examResultRepository.findAllByUserIdAndExamIdAndLastOpenerUsageDateIsNotNull(userId, examId);

        // 3. ExamResult가 존재하지 않으면 null 반환
        if (examResults.isEmpty()) {
            log.info("스크랩북 분류 선택 후 리스트 조회 실패 - 해당 시험에 대한 ExamResult가 존재하지 않음. userId: {}, examId: {}", userId, examId);
            // 빈 QuestionResult 페이지와 함께 ScrapbookResponse 반환
            return ScrapbookResponse.builder()
                        .examType(exam.getExamType())
                        .examYear(exam.getExamYear())
                        .questionResults(PageResponse.from(new PageImpl<> (Collections.emptyList(), pageable, 0) ))
                        .build();
        }

        // 4. 존재하면 QuestionResult 목록 조회 및 ScrapbookResponse 생성 후 반환
        // 4-1. Id 목록 추출
        List<Long> examResultIds = examResults.stream()
                .map(ExamResult::getId)
                .toList();

        log.info("조회된 ExamResult IDs: {}", examResultIds);

        // 4-2. QuestionResult 목록 조회
        Page<QuestionResult> questionResults = questionResultRepository
                .findAllByExamResultIdInAndIsOpenerIsTrue(examResultIds, pageable);

        // 4-3. 로그 출력
        questionResults.forEach(qr ->
                log.debug("조회된 QuestionResult - id: {}, questionId: {}, examResultId: {}",
                        qr.getId(), qr.getQuestion().getId(), qr.getExamResult().getId())
        );


        log.info("스크랩북 분류 선택 후 리스트 조회 성공 - userId: {}, examId: {}, 조회된 QuestionResult 개수: {}",
                userId, examId, questionResults.getNumberOfElements());

        // 4-4. ScrapbookQuestionResult 생성 및 반환
        List<ScrapbookQuestionResult> scrapbookQuestionResults = questionResults.stream()
                .map(ScrapbookMapper::toScrapbookQuestionResult)
                .toList();


        // 4-5. ScrapbookResponse 생성 및 반환
        return ScrapbookResponse.builder()
                .examType(exam.getExamType())
                .examYear(exam.getExamYear())
                .questionResults(PageResponse.from(
                        new PageImpl<>(scrapbookQuestionResults, pageable, questionResults.getTotalElements())))
                .build();
    }
}
