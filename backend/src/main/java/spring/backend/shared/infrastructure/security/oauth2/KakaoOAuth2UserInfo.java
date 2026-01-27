package spring.backend.shared.infrastructure.security.oauth2;

import java.util.Map;

/**
 * 카카오 OAuth2 사용자 정보
 */
public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getProviderId() {
        Object id = attributes.get("id");
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public String getName() {
        // kakao_account.profile.nickname 경로로 접근
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) {
            // null 처리방지
            return "카카오 사용자";
        }

        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        if (profile == null) {
            // null 처리방지
            return "카카오 사용자";
        }

        String nickname = (String) profile.get("nickname");
        return nickname != null ? nickname : "카카오 사용자";
    }

}
