package spring.backend.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.backend.domain.auth.dto.request.FormSignupRequest;
import spring.backend.domain.auth.dto.response.AuthTokens;
import spring.backend.domain.auth.dto.response.TokenResponse;
import spring.backend.domain.auth.service.spec.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/form-signup")
  public TokenResponse formSignup(
          @Valid @RequestBody FormSignupRequest req,
          HttpServletResponse response) {

    // Service에서 accessToken과 refreshToken 받기
    AuthTokens tokens = authService.formSignup(req);

    // Refresh Token을 HttpOnly Cookie에 담기
    ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.refreshToken())
            .httpOnly(true)
            .secure(true)          // HTTPS 환경에서만
            .sameSite("Strict")    // or Lax
            .path("/auth/refresh") // 재발급 API에만 전송
            .maxAge(Duration.ofDays(14)) // 만료시간 설정
            .build();

    // 쿠키를 응답 헤더에 추가
    response.addHeader("Set-Cookie", refreshCookie.toString());

    // AccessToken만 body로 반환
    return new TokenResponse(tokens.accessToken());
  }
}
