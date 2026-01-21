package spring.backend.shared.infrastructure.security.oauth2;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import spring.backend.domain.auth.model.enums.Provider;
import spring.backend.domain.user.model.enums.Role;

/**
 * Spring Security OAuth2User 커스텀 구현 OAuth2 인증 후 사용자 정보를 담는 클래스
 */
@Getter
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User {

    private final UUID userId;
    private final String name;
    private final Role role;
    private final Provider provider;
    private final String providerId;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return name;
    }
}
