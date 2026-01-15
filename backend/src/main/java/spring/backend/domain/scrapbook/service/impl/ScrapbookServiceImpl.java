package spring.backend.domain.scrapbook.service.impl;


import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.exam.model.dto.ExamResultSearchCriteria;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.domain.scrapbook.dto.request.ScrapbookFilterSearchRequest;
import spring.backend.domain.scrapbook.dto.response.ScrapbookFilterResponse;
import spring.backend.domain.scrapbook.mapper.ScrapbookMapper;
import spring.backend.domain.scrapbook.service.spec.ScrapbookService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ScrapbookServiceImpl implements ScrapbookService {

    private final ExamResultRepository examResultRepository;

    public ScrapbookServiceImpl(ExamResultRepository examResultRepository) {
        this.examResultRepository = examResultRepository;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ScrapbookFilterResponse> searchScrapbookFilters(UUID userId, ScrapbookFilterSearchRequest request, Pageable pageable) {
        log.info("ScrapbookServiceImpl.searchScrapbookFilters - userId: {}, request: {}, pageable: {}", userId, request, pageable);
        // ExamResultSearchCriteria로 변환하여 ExamResultRepository의 검색 메서드 호출
        Page<ExamResult> examResults = examResultRepository.searchExamResults(
                toExamResultSearchCriteria(userId, request),
                pageable
        );

        // ExamResult를 ScrapbookFilterResponse로 매핑
        List<ScrapbookFilterResponse> responses = examResults.stream().map(ScrapbookMapper::toScrapbookFilterResponse).toList();

        // Page<ScrapbookFilterResponse> 반환
        return new PageImpl<>(responses, pageable, examResults.getTotalElements());
    }

    private ExamResultSearchCriteria toExamResultSearchCriteria(UUID userId, ScrapbookFilterSearchRequest request) {
        return ExamResultSearchCriteria.builder()
                    .userId(userId)
                    .examType(request.getExamType())
                    .examYear(request.getExamYear()==0 ? null : request.getExamYear())
                    .openerUsage(true) // 스크랩북은 무조건 문제풀이 기록이 있는 것만 조회
                    .createdAtFrom(request.getStartDate())
                    .createdAtTo(request.getEndDate())
                    .build();
    }
}
