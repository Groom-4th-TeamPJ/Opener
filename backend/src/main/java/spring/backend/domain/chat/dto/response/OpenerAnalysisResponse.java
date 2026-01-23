package spring.backend.domain.chat.dto.response;

import lombok.Builder;

/**
 * 오프너 분석 응답 DTO
 */
@Builder
public record OpenerAnalysisResponse(
        String data  // LLM 분석 결과 (마크다운 형식)
) {
}
