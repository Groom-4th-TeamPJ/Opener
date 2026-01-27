package spring.backend.domain.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.backend.domain.auth.dto.request.FormSignupRequest;
import spring.backend.domain.auth.dto.request.OAuthSignupRequest;
import spring.backend.domain.auth.service.spec.AuthService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/form-signup")
  public void formSignup(
          @Valid @RequestBody FormSignupRequest req,
          HttpServletResponse response) {

    // Service에서 accessToken과 refreshToken response의 HttpOnly에 담아 반환
    authService.formSignup(response, req);
  }

  @PostMapping("/oauth-signup")
  public void oAuthSignup(
          @Valid @RequestBody OAuthSignupRequest req,
          HttpServletResponse response) {

    // signupToken 검증 후 OAuth 회원가입 처리
    authService.oAuthSignup(response, req);
  }

  @PostMapping("/refresh")
  public void tokenRefresh(
          @CookieValue(name = "refreshToken", required = false) String refreshToken,
          HttpServletResponse response
  ) {

    if (refreshToken == null || refreshToken.isBlank()) {
      throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    authService.tokenRefresh(response, refreshToken);
  }

  @PostMapping("/logout")
  public void logout(HttpServletRequest req, HttpServletResponse res) {
    authService.logout(req, res);
  }
}
