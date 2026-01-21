package spring.backend.shared.infrastructure.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import spring.backend.shared.infrastructure.security.oauth2.CustomOAuth2User;
import spring.backend.shared.infrastructure.security.util.JwtUtil;

import java.io.IOException;

/**
 * OAuth2 인증 성공 핸들러
 * JWT 토큰 생성 및 프론트엔드로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  private final JwtUtil jwtUtil;

  @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
  private String redirectUri;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication
  ) throws IOException, ServletException {

    CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

    log.info("OAuth2 인증 성공: userId={}, name={}",
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

    // 프론트엔드로 리다이렉트
    getRedirectStrategy().sendRedirect(request, response, redirectUri);
  }
}
