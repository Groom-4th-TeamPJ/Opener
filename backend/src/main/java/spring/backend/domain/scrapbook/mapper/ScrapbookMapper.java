package spring.backend.domain.scrapbook.mapper;

import spring.backend.domain.exam.model.dto.Passage;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.exam.model.enums.PassageType;
import spring.backend.domain.scrapbook.dto.response.ScrapbookQuestionResult;

import java.util.Comparator;
import java.util.List;

public class ScrapbookMapper {

    public static ScrapbookQuestionResult toScrapbookQuestionResult(QuestionResult qr) {
        List<Passage> passages = new java.util.ArrayList<>(List.copyOf(qr.getQuestion().getPassages()));
        // passages order 기준으로 정렬
        passages.sort(Comparator.comparingInt(Passage::order));

        StringBuilder question = new StringBuilder();

        // passages 를 type 이 TEXT이면  question 문자열 생성
        for (Passage p : passages) {
            if (p.type().equals(PassageType.TEXT)) {
                question.append(p.content());
                question.append(" ");
            }
        }

        return ScrapbookQuestionResult.builder()
                .questionResultId(qr.getId())
                .category(qr.getQuestion().getCategory())
                .questionNo(qr.getQuestion().getQuestionNo())
//                .passages(qr.getQuestion().getPassages())
                .passage(question.toString().trim())
                .openerUsedAt(qr.getOpenerUsedAt())
                .build();
    }

}
