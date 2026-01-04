package spring.backend.shared.infrastructure.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import spring.backend.domain.auth.dto.response.AccessToken;
import spring.backend.domain.auth.model.entity.Credentials;
import spring.backend.domain.auth.respository.spec.CredentialRepository;
import spring.backend.domain.user.model.entity.User;
import spring.backend.shared.infrastructure.security.util.JwtUtil;
import spring.backend.shared.response.codes.SuccessCode;
import spring.backend.shared.response.format.ApiResponseFormat;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtUtil jwtUtil;
  private final CredentialRepository credentialRepository;
  private final ObjectMapper objectMapper;

  @Override
  @Transactional
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

    // 로그인 성공 처리: lastLoginAt 업데이트 + 실패 카운트 리셋
    // Transactional이라 save() 안해도 영속성 컨텍스트가 자동저장
    credentials.recordLoginSuccess();

    // 조회한 credentials로 user 조회
    User user = credentials.getUser();

    // JWT 토큰 생성
    String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole(), user.getName());
    String refreshToken = jwtUtil.generateRefreshToken(user.getId());

    // TokenResponse 생성(AccessToken만 반환)
    AccessToken tokenResponse = new AccessToken(accessToken);

    // Refresh Token을 HttpOnly Cookie에 담기
    ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(false)         // 로컬 개발용 (프로덕션에서는 true)
            .sameSite("Lax")       // Strict보다 완화된 정책
            .path("/api/auth/refresh")     // /api/auth 하위 모든 엔드포인트에서 사용 가능
            .maxAge(Duration.ofDays(14)) // 만료시간 설정
            .build();

    // 공통 응답 포맷으로 래핑
    ApiResponseFormat<AccessToken> apiResponse = ApiResponseFormat.success(
            SuccessCode.OK.getCode(),
            SuccessCode.OK.getMessage(),
            tokenResponse
    );

    // Refresh Token 쿠키를 응답 헤더에 추가
    response.addHeader("Set-Cookie", refreshCookie.toString());

    // JSON 응답 반환
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), apiResponse);
  }
}
