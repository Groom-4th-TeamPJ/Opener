package spring.backend.domain.auth.service.impl;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import spring.backend.domain.auth.dto.request.FormSignupRequest;
import spring.backend.domain.auth.dto.request.OAuthSignupRequest;
import spring.backend.domain.auth.model.entity.Credentials;
import spring.backend.domain.auth.respository.jpa.JpaCredentialRepository;
import spring.backend.domain.auth.respository.spec.CredentialRepository;
import spring.backend.domain.auth.service.spec.AuthService;
import spring.backend.domain.user.model.entity.User;
import spring.backend.domain.user.repository.spec.UserRepository;
import spring.backend.shared.infrastructure.security.util.JwtUtil;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

  private final CredentialRepository credentialRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final JpaCredentialRepository jpaCredentialRepository;
  private final UserRepository userRepository;

  @Qualifier("authRedisTemplate")
  private final StringRedisTemplate redisTemplate;

  @Override
  public void formSignup(HttpServletResponse response, FormSignupRequest req) {

    // 이메일 중복 확인
    if (credentialRepository.existsByEmail(req.email())) {
      throw new IllegalArgumentException("이미 존재하는 계정"); // 이후 공통 응답으로 수정
    }

    // User 생성
    User user = User.createUser(req.name());

    // Credential 생성
    String encodedPassword = passwordEncoder.encode(req.password());

    Credentials credential = Credentials.createFormCredentials(
            user,
            req.email(),
            encodedPassword
    );

    // 저장
    Credentials newCredential = jpaCredentialRepository.save(credential);

    // 토큰 생성
    String accessToken = jwtUtil.generateAccessToken(
            newCredential.getUser().getId(),
            newCredential.getUser().getRole(),
            newCredential.getUser().getName());
    String refreshToken = jwtUtil.generateRefreshToken(newCredential.getUser().getId());

    jwtUtil.setHttpOnlyAllToken(response, accessToken, refreshToken);
  }

  @Override
  public void oauthSignup(OAuthSignupRequest req) {
  }

  @Override
  public void logout(HttpServletRequest req) {

    String bearerToken = jwtUtil.extractAccessTokenFromRequest(req);

    Claims claim = jwtUtil.validateToken(bearerToken);

    long ttl = Math.max(
            (claim.getExpiration().getTime() - System.currentTimeMillis()) / 1000,
            0
    );

    redisTemplate.opsForValue()
            .set(
                    "blacklist:access:" + claim.getId(), // ⭐ get("jti") 말고 getId()
                    "logout",
                    ttl,
                    TimeUnit.SECONDS
            );
  }

  @Override
  public void tokenRefresh(HttpServletResponse response, String refreshToken) {

    // refresh 검증
    Claims claims = jwtUtil.validateToken(refreshToken);

    UUID userId = UUID.fromString(claims.getSubject());

    String redisKey = "refreshToken:" + userId;
    String refreshTokenInRedis = (String) redisTemplate.opsForValue().get(redisKey);

    if (!refreshToken.equals(refreshTokenInRedis)) {
      throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh Token이 Redis와 일치하지 않습니다.");
    }

    // user 조회
    User user = userRepository.findUserById(userId);

    // 조회 데이터 기반 access 재생성
    String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole(), user.getName());

    jwtUtil.setHttpOnlyAccessToken(response, newAccessToken);

  }
}
