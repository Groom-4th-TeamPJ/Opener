package spring.backend.shared.infrastructure.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
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

    String jti = UUID.randomUUID().toString();

    return Jwts.builder()
            .claims(Jwts.claims()
                    .subject(userId.toString())
                    .id(jti)
                    .add("role", role)
                    .add("name", name)
                    .build()
            )
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

  public String extractAccessTokenFromRequest(HttpServletRequest request) {
    return findCookieValue(request.getCookies(), "accessToken");
  }

  public String extractRefreshTokenFromRequest(HttpServletRequest request) {
    return findCookieValue(request.getCookies(), "refreshToken");
  }

  private String findCookieValue(Cookie[] cookies, String cookieName) {

    if (cookies == null) {
      return null;
    }

    for (Cookie cookie : cookies) {
      if (cookieName.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }

    return null;
  }

  public void setHttpOnlyAllToken(HttpServletResponse response, String accessToken, String refreshToken) {

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
  }

  public void setHttpOnlyAccessToken(HttpServletResponse response, String accessToken) {

    // Access Token HttpOnly에 적재
    ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
            .httpOnly(true)
            .secure(true)
            .path("/api/")
            .sameSite("Lax")
            .maxAge(Duration.ofMinutes(60)) // 수명 : 1시간
            .build();

    response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
  }
}
