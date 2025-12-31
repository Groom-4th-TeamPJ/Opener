package spring.backend.domain.auth.dto.response;

public record SignupResponse(
        String accessToken,
        String refreshToken
) {
}
