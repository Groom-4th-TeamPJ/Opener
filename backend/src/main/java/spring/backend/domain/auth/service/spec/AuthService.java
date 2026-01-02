package spring.backend.domain.auth.service.spec;

import spring.backend.domain.auth.dto.request.FormSignupRequest;
import spring.backend.domain.auth.dto.request.OAuthSignupRequest;
import spring.backend.domain.auth.dto.response.TokenResponse;

public interface AuthService {

  TokenResponse formSignup(FormSignupRequest req);

  TokenResponse oauthSignup(OAuthSignupRequest req);

  void logout(String accessToken, String refreshToken);

  TokenResponse refreshToken(String refreshToken);

}
