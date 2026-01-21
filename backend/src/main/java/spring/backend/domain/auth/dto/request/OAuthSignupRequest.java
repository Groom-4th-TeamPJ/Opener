package spring.backend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * OAuth2 회원가입 요청 DTO
 */
public record OAuthSignupRequest(
        @NotBlank(message = "회원가입 토큰은 필수입니다")
        String signupToken,

        @NotBlank(message = "이름은 필수입니다")
        @Size(max = 100, message = "이름은 100자 이하여야 합니다")
        String name
) {
}
