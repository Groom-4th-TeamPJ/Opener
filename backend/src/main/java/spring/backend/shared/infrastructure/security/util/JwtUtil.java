package spring.backend.shared.infrastructure.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.Value;
import org.flywaydb.core.internal.util.StringUtils;
import org.springframework.stereotype.Component;
import spring.backend.domain.user.model.enums.Role;
import spring.backend.shared.infrastructure.security.dto.AuthUser;

@Component
public class JwtUtil {

  private final SecretKey secretKey;
  private final long accessTokenExpiration;
  private final long refreshTokenExpiration;

  public JwtUtil(
          @Value("${jwt.secret}") String secretKey,
          @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
          @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
  ) {
    this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  // Access Token 생성 (userId, name, role 포함)
  public String generateAccessToken(UUID userId, Role role, String name) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + accessTokenExpiration);

    return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role)
            .claim("name", name)  // 사용자 이름 추가
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact();
  }

  // Refresh Token 생성 (userId만 포함)
  public String generateRefreshToken(UUID userId) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + refreshTokenExpiration);

    return Jwts.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiration(expiration)
            .signWith(secretKey)
            .compact();
  }

  // 토큰에서 userId, name 추출하여 AuthUser dto로 만들어 반환
  public AuthUser getUserInfoFromToken(Claims claims) {
    // 타입 직렬화
    UUID userId = UUID.fromString(claims.getSubject());
    String userName = claims.get("name", String.class);

    return new AuthUser(userId, userName);
  }

  // 토큰 유효성 검증
  public Claims validateToken(String token) {
    return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
  }

  public String extractTokenFormRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");

    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7); // "Bearer " 문자 제거
    }

    return null;
  }
}
