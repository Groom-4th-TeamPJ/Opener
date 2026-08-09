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
import spring.backend.domain.auth.model.enums.Provider;
import spring.backend.domain.user.model.enums.Role;
import spring.backend.shared.infrastructure.security.dto.AuthUser;
import spring.backend.shared.infrastructure.security.dto.OAuthSignupInfo;

@Component
public class JwtUtil {

    private static final long SIGNUP_TOKEN_EXPIRATION = 600000L; // 10분

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final boolean cookieSecure;
    private final String cookieSameSite;

    public JwtUtil(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            @Value("${app.cookie.secure:true}") boolean cookieSecure,
            @Value("${app.cookie.same-site:None}") String cookieSameSite
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;
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

    // OAuth2 Signup Token 생성 (회원가입 전 임시 토큰)
    public String generateSignupToken(Provider provider, String providerId, String name) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + SIGNUP_TOKEN_EXPIRATION);

        return Jwts.builder()
                .claims(Jwts.claims()
                        .subject(providerId)
                        .add("provider", provider.name())
                        .add("name", name)
                        .add("type", "signup")
                        .build()
                )
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    // Signup Token에서 OAuth 정보 추출
    public OAuthSignupInfo getOAuthInfoFromSignupToken(String token) {
        Claims claims = validateToken(token);

        String type = claims.get("type", String.class);
        if (!"signup".equals(type)) {
            throw new IllegalArgumentException("유효하지 않은 회원가입 토큰입니다.");
        }

        String providerId = claims.getSubject();
        Provider provider = Provider.valueOf(claims.get("provider", String.class));
        String name = claims.get("name", String.class);

        return new OAuthSignupInfo(provider, providerId, name);
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
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite)
                // 쿠키 maxAge 를 JWT exp 와 같은 설정에서 계산 -> 리터럴로 두면 설정만 줄였을 때
                // 토큰은 만료됐는데 쿠키만 살아남아 "쿠키는 있는데 401" 이 설정 파일에 안 보인다
                .maxAge(Duration.ofMillis(accessTokenExpiration))
                .build();

        // Refresh Token HttpOnly에 적재
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth/refresh")
                .sameSite(cookieSameSite)
                // 쿠키 maxAge 를 JWT exp 와 같은 설정에서 계산 -> 세 수명(exp·Redis TTL·쿠키)이 어긋나
                // 저장본이나 쿠키가 먼저 사라져 재발급이 실패하던 창을 구조로 막는다
                .maxAge(Duration.ofMillis(refreshTokenExpiration))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    public void setHttpOnlyAccessToken(HttpServletResponse response, String accessToken) {

        // Access Token HttpOnly에 적재
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite)
                // 재발급 경로도 같은 설정에서 계산 -> 두 발급 지점 중 한쪽만 리터럴이면 수명이 다시 갈라진다
                .maxAge(Duration.ofMillis(accessTokenExpiration))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
    }

    /**
     * 로그아웃 시 httpOnly 쿠키 삭제 maxAge(0)으로 설정하여 브라우저에서 즉시 삭제되도록 함
     */
    public void clearAllTokenCookies(HttpServletResponse response) {
        // Access Token 쿠키 삭제 (path="/"로 설정된 쿠키)
        ResponseCookie clearAccessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite)
                .maxAge(0) // 즉시 만료
                .build();

        // Refresh Token 쿠키 삭제 (path="/api/auth/refresh"로 설정된 쿠키)
        ResponseCookie clearRefreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth/refresh")
                .sameSite(cookieSameSite)
                .maxAge(0) // 즉시 만료
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, clearAccessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString());
    }
}
