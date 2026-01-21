package spring.backend.shared.infrastructure.security.oauth2;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.auth.model.entity.Credentials;
import spring.backend.domain.auth.model.enums.Provider;
import spring.backend.domain.auth.respository.spec.CredentialRepository;
import spring.backend.domain.user.model.entity.User;
import spring.backend.domain.user.model.enums.Role;

/**
 * Spring Security OAuth2 사용자 서비스
 * OAuth2 인증 후 사용자 정보를 로드 (회원가입은 별도 엔드포인트에서 처리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final CredentialRepository credentialRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. OAuth2 제공자로부터 사용자 정보 조회
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 로그인 시작: provider={}", registrationId);

        // 2. 제공자별 사용자 정보 파싱
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId,
                oAuth2User.getAttributes()
        );

        String providerId = oAuth2UserInfo.getProviderId();
        String name = oAuth2UserInfo.getName();
        Provider provider = Provider.valueOf(registrationId.toUpperCase());

        log.debug("OAuth2 사용자 정보: providerId={}, name={}", providerId, name);

        // 3. providerId로 기존 사용자 조회
        Credentials credentials = credentialRepository.findUserCredentialByProviderId(providerId)
                .orElse(null);

        // 4. 기존 회원인 경우
        if (credentials != null) {
            log.info("기존 OAuth2 사용자 로그인: userId={}, providerId={}",
                    credentials.getUser().getId(), providerId);

            User user = credentials.getUser();
            return new CustomOAuth2User(
                    user.getId(),
                    user.getName(),
                    user.getRole(),
                    provider,
                    providerId,
                    oAuth2User.getAttributes(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                    false  // 기존 회원
            );
        }

        // 5. 신규 회원인 경우 - 회원가입하지 않고 정보만 반환
        log.info("신규 OAuth2 사용자 감지: providerId={}, name={}", providerId, name);

        return new CustomOAuth2User(
                null,   // userId는 아직 없음
                name,
                Role.USER,
                provider,
                providerId,
                oAuth2User.getAttributes(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),
                true    // 신규 회원
        );
    }
}
