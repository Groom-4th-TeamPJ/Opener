package spring.backend.shared.infrastructure.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import spring.backend.domain.auth.model.entity.Credentials;
import spring.backend.domain.auth.respository.spec.CredentialRepository;
import spring.backend.domain.user.model.entity.User;
import spring.backend.shared.infrastructure.security.util.JwtUtil;
import spring.backend.shared.response.codes.SuccessCode;
import spring.backend.shared.response.format.ApiResponseFormat;


@Component
public class FormAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  private final JwtUtil jwtUtil;
  private final CredentialRepository credentialRepository;
  private final ObjectMapper objectMapper;
  private final StringRedisTemplate redisTemplate;

  public FormAuthenticationSuccessHandler(
          JwtUtil jwtUtil,
          CredentialRepository credentialRepository,
          ObjectMapper objectMapper,
          @Qualifier("authRedisTemplate") StringRedisTemplate redisTemplate
  ) {
    this.jwtUtil = jwtUtil;
    this.credentialRepository = credentialRepository;
    this.objectMapper = objectMapper;
    this.redisTemplate = redisTemplate;
  }

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

    // Access Token HttpOnly에 적재
    ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
            .httpOnly(true)
            .secure(true)
            .path("/api/")
            .sameSite("Lax")
            .maxAge(Duration.ofMinutes(60)) // 수명 : 1시간
            .build();

    // Refresh Token HttpOnly에 적재
    ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(false)
            .path("/api/auth/refresh")
            .sameSite("Lax")
            .maxAge(Duration.ofDays(7)) // 수명 : 7일
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

    // 공통 응답 포맷으로 래핑 (data는 null)
    ApiResponseFormat<Void> apiResponse = ApiResponseFormat.success(
            SuccessCode.OK.getCode(),
            SuccessCode.OK.getMessage()
    );

    // JSON 응답 반환
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), apiResponse);

    // Auth Redis에 refresh Token 캐싱
    String redisKey = "refreshToken:" + user.getId();
    redisTemplate.opsForValue().set(redisKey, refreshToken, 7, TimeUnit.DAYS);
  }
}
