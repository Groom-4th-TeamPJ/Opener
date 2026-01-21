package spring.backend.shared.infrastructure.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import spring.backend.shared.infrastructure.security.oauth2.CustomOAuth2User;
import spring.backend.shared.infrastructure.security.util.JwtUtil;

/**
 * OAuth2 인증 성공 핸들러
 * - 기존 회원: JWT 토큰 발급 후 메인 페이지로 리다이렉트
 * - 신규 회원: signupToken 발급 후 회원가입 페이지로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtUtil jwtUtil;

  @Value("${app.oauth2.signup-redirect-uri}")
  private String signupRedirectUri;

  @Value("${app.oauth2.login-success-uri}")
  private String loginSuccessUri;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) throws IOException, ServletException {

    CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

    if (oAuth2User.isNewUser()) {
      // 신규 회원: signupToken 발급 후 회원가입 페이지로 리다이렉트
      handleNewUser(request, response, oAuth2User);
    } else {
      // 기존 회원: JWT 발급 후 메인 페이지로 리다이렉트
      handleExistingUser(request, response, oAuth2User);
    }
  }

  private void handleNewUser(
      HttpServletRequest request,
      HttpServletResponse response,
      CustomOAuth2User oAuth2User
  ) throws IOException {
    log.info("신규 OAuth2 사용자, 회원가입 페이지로 리다이렉트: provider={}, providerId={}",
        oAuth2User.getProvider(), oAuth2User.getProviderId());

    // signupToken 생성
    String signupToken = jwtUtil.generateSignupToken(
        oAuth2User.getProvider(),
        oAuth2User.getProviderId(),
        oAuth2User.getName()
    );

    // 회원가입 페이지로 리다이렉트 (signupToken을 쿼리 파라미터로 전달)
    String encodedName = URLEncoder.encode(oAuth2User.getName(), StandardCharsets.UTF_8);
    String redirectUrl = signupRedirectUri + "?signupToken=" + signupToken + "&name=" + encodedName;

    getRedirectStrategy().sendRedirect(request, response, redirectUrl);
  }

  private void handleExistingUser(
      HttpServletRequest request,
      HttpServletResponse response,
      CustomOAuth2User oAuth2User
  ) throws IOException {
    log.info("기존 OAuth2 사용자 로그인 성공: userId={}, name={}",
        oAuth2User.getUserId(), oAuth2User.getName());

    // JWT 토큰 생성
    String accessToken = jwtUtil.generateAccessToken(
        oAuth2User.getUserId(),
        oAuth2User.getRole(),
        oAuth2User.getName()
    );
    String refreshToken = jwtUtil.generateRefreshToken(oAuth2User.getUserId());

    // HttpOnly 쿠키에 토큰 설정
    jwtUtil.setHttpOnlyAllToken(response, accessToken, refreshToken);

    log.info("OAuth2 JWT 토큰 발급 완료: userId={}", oAuth2User.getUserId());

    // 메인 페이지로 리다이렉트
    getRedirectStrategy().sendRedirect(request, response, loginSuccessUri);
  }
}
