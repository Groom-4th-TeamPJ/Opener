package spring.backend.domain.auth.dto.response;

// 내부용 DTO
public record AuthTokens(
        String accessToken,
        String refreshToken
) {
}
