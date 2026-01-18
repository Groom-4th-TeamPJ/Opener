package spring.backend.domain.chat.dto.response;

import java.util.List;
import lombok.Builder;
import spring.backend.domain.chat.model.vo.Option;
import spring.backend.domain.chat.model.vo.Passage;

/**
 * 변형 문제 생성 응답 DTO
 */
@Builder
public record GenerateQuestionResponse(
        List<Passage> passages,          // 문제 지문
        List<Option> options,            // 선택지
        Integer answer,                  // 정답 번호
        String analysis                  // 풀이 방법
) {
}
