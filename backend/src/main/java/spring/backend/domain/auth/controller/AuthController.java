package spring.backend.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "👤 Auth", description = "사용자 회원가입 / 로그인 / 로그아웃")
public class AuthController {

  private final AuthService authService;
  @Operation(
          summary = "폼 회원가입",
          description = "이메일과 비밀번호를 사용해 신규 회원을 등록합니다.\n" +
                  "회원가입 성공 후 바로 로그인 상태가 됩니다."
  )
  @PostMapping("/form-signup")
  public void formSignup(
          @Valid @RequestBody FormSignupRequest req,
          HttpServletResponse response) {

    // Service에서 accessToken과 refreshToken response의 HttpOnly에 담아 반환
    authService.formSignup(response, req);
  }
  @Operation(
          summary = "OAuth 회원가입",
          description = "OAuth2.0 로그인을 통해 가입한 사용자가 추가 정보를 입력해 회원가입을 완료합니다.\n" +
                  "회원가입 성공 후 바로 로그인 상태가 됩니다."
  )
  @PostMapping("/oauth-signup")
  public void oAuthSignup(
          @Valid @RequestBody OAuthSignupRequest req,
          HttpServletResponse response) {

    // signupToken 검증 후 OAuth 회원가입 처리
    authService.oAuthSignup(response, req);
  }

  @Operation(
          summary = "Access Token 재발급",
            description = "만료된 Access Token을 재발급합니다.\n" +
                    "재발급된 Access Token은 HttpOnly 쿠키에 담겨 반환됩니다."
  )
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

  @Operation(
          summary = "로그아웃",
            description = "사용자 로그아웃을 처리합니다.\n" +
                    "로그아웃 시 클라이언트에 저장된 Access Token과 Refresh Token이 모두 삭제됩니다."
  )
  @PostMapping("/logout")
  public void logout(HttpServletRequest req, HttpServletResponse res) {
    authService.logout(req, res);
  }
}
