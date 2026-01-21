package spring.backend.shared.infrastructure.security.dto;

import spring.backend.domain.auth.model.enums.Provider;

/**
 * OAuth2 회원가입 토큰에서 추출한 정보
 */
public record OAuthSignupInfo(
        Provider provider,
        String providerId,
        String name
) {
}
