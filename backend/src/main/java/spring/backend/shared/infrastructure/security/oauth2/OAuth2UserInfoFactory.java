package spring.backend.shared.infrastructure.security.oauth2;

import java.util.Map;
import spring.backend.domain.auth.model.enums.Provider;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

/**
 * OAuth2UserInfo 팩토리 Provider에 따라 적절한 OAuth2UserInfo 구현체 생성
 */
public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        Provider provider;

        try {
            provider = Provider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_OAUTH_PROVIDER);
        }

        switch (provider) {
            case KAKAO:
                return new KakaoOAuth2UserInfo(attributes);
            // 향후 Google 추가 가능
            // case GOOGLE:
            //   return new GoogleOAuth2UserInfo(attributes);
            default:
                throw new BusinessException(ErrorCode.INVALID_OAUTH_PROVIDER);
        }
    }
}
