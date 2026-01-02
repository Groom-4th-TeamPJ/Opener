package spring.backend.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.backend.domain.auth.dto.request.FormSignupRequest;
import spring.backend.domain.auth.dto.response.AccessToken;
import spring.backend.domain.auth.dto.response.AuthTokens;
import spring.backend.domain.auth.service.spec.AuthService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/form-signup")
  public AccessToken formSignup(
          @Valid @RequestBody FormSignupRequest req,
          HttpServletResponse response) {

    // Service에서 accessToken과 refreshToken 받기
    AuthTokens tokens = authService.formSignup(req);

    // Refresh Token을 HttpOnly Cookie에 담기
    ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
            .httpOnly(true)
            .secure(false)         // 로컬 개발용 (프로덕션에서는 true)
            .sameSite("Lax")       // Strict보다 완화된 정책
            .path("/api/auth/refresh")     // /api/auth 하위 모든 엔드포인트에서 사용 가능
            .maxAge(Duration.ofDays(14)) // 만료시간 설정
            .build();

    // 쿠키를 응답 헤더에 추가
    response.addHeader("Set-Cookie", refreshCookie.toString());

    // AccessToken만 body로 반환
    return new AccessToken(tokens.accessToken());
  }

  @PostMapping("/refresh")
  public AccessToken tokenRefresh(
          @CookieValue(name = "refreshToken", required = false) String refreshToken) {

    if (refreshToken == null || refreshToken.isBlank()) {
      throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    return authService.tokenRefresh(refreshToken);
  }
}
