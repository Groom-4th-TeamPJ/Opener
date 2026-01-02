package spring.backend.shared.infrastructure.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import spring.backend.domain.auth.dto.response.TokenResponse;
import spring.backend.domain.auth.model.entity.Credentials;
import spring.backend.domain.auth.respository.spec.CredentialRepository;
import spring.backend.domain.user.model.entity.User;
import spring.backend.shared.infrastructure.security.util.JwtUtil;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtUtil jwtUtil;
  private final CredentialRepository credentialRepository;
  private final ObjectMapper objectMapper;

  @Override
  public void onAuthenticationSuccess(
          HttpServletRequest request,
          HttpServletResponse response,
          Authentication authentication
  ) throws IOException {

    // 인증된 사용자 정보에서 email 추출
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String email = userDetails.getUsername();

    // email로 Credentials 및 User 정보 조회
    Credentials credentials = credentialRepository
            .findUserCredentialByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    User user = credentials.getUser();

    // JWT 토큰 생성
    String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole(), user.getName());
    String refreshToken = jwtUtil.generateRefreshToken(user.getId());

    // TokenResponse 생성
    TokenResponse tokenResponse = new TokenResponse(accessToken, refreshToken);

    // JSON 응답 반환
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), tokenResponse);
  }
}
