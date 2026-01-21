package spring.backend.domain.auth.service.spec;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import spring.backend.domain.auth.dto.request.FormSignupRequest;
import spring.backend.domain.auth.dto.request.OAuthSignupRequest;

public interface AuthService {

  void formSignup(HttpServletResponse response, FormSignupRequest req);

  void oAuthSignup(HttpServletResponse response, OAuthSignupRequest req);

  void logout(HttpServletRequest req);

  void tokenRefresh(HttpServletResponse response, String refreshToken);

}
