package spring.backend.domain.chat.dto.request;

public record OpenerAnalysisRequest(
        Long sessionId,
        Long questionResultId,
        Long questionId
) implements SessionScoped {
}
