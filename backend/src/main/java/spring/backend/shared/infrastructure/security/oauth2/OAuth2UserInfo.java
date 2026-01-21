package spring.backend.shared.infrastructure.security.oauth2;

import java.util.Map;

/**
 * OAuth2 제공자별 사용자 정보 추상 클래스 각 제공자(Kakao, Google 등)의 응답 구조가 다르므로 추상화
 */
public abstract class OAuth2UserInfo {

    protected Map<String, Object> attributes;

    public OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * OAuth 제공자가 제공하는 고유 ID
     */
    public abstract String getProviderId();

    /**
     * 사용자 이름
     */
    public abstract String getName();
  
}
