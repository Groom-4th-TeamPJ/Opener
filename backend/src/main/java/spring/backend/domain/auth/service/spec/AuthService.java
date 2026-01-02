package spring.backend.domain.auth.service.spec;

import spring.backend.domain.auth.dto.request.FormSignupRequest;
import spring.backend.domain.auth.dto.request.OAuthSignupRequest;
import spring.backend.domain.auth.dto.response.AuthTokens;

public interface AuthService {

  AuthTokens formSignup(FormSignupRequest req);

  AuthTokens oauthSignup(OAuthSignupRequest req);

  void logout(String accessToken, String refreshToken);

  AuthTokens refreshToken(String refreshToken);

}
