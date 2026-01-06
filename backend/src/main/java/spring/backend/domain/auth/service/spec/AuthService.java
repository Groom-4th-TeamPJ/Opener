package spring.backend.domain.auth.service.spec;

import jakarta.servlet.http.HttpServletRequest;
import spring.backend.domain.auth.dto.request.FormSignupRequest;
import spring.backend.domain.auth.dto.request.OAuthSignupRequest;
import spring.backend.domain.auth.dto.response.AccessToken;
import spring.backend.domain.auth.dto.response.AuthTokens;

public interface AuthService {

  AuthTokens formSignup(FormSignupRequest req);

  AuthTokens oauthSignup(OAuthSignupRequest req);

  void logout(HttpServletRequest req);

  AccessToken tokenRefresh(String refreshToken);

}
