package spring.backend.domain.scrapbook.mapper;

import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.scrapbook.dto.response.ScrapbookFilterResponse;

public class ScrapbookMapper {

     public static ScrapbookFilterResponse toScrapbookFilterResponse(ExamResult examResult) {
        return ScrapbookFilterResponse.builder()
                .examResultId(examResult.getId())
                .examYear(examResult.getExam().getExamYear())
                .examType(examResult.getExam().getExamType())
                .openerUsageCount(examResult.getOpenerUsageCount())
                .recentDate(examResult.getCreatedAt().toLocalDate())
                .build();
    }
}
