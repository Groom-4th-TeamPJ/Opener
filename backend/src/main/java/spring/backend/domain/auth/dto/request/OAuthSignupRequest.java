package spring.backend.domain.auth.dto.request;

// TODO: OAuth 회원가입 요청 DTO 구현 필요
public record OAuthSignupRequest(
        String providerId,
        String provider
) {
}
