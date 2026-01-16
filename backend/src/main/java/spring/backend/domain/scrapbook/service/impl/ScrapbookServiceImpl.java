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
    public List<ScrapbookFilterResponse> getScrapbookFilters(UUID userId) {
        return examResultRepository.findScrapbookFiltersByUserId(userId);
    }
}
