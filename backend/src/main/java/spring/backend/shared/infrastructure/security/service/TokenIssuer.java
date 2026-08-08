package spring.backend.shared.infrastructure.security.service;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import spring.backend.domain.user.model.enums.Role;
import spring.backend.shared.infrastructure.security.util.JwtUtil;

// 토큰 발급의 유일한 진입점
// 발급 코드가 4곳에 복제돼 있으면 그중 한 곳만 Redis 저장을 빠뜨려도 컴파일도 테스트도 통과한다
@Slf4j
@Component
public class TokenIssuer {

    private static final String REFRESH_KEY_PREFIX = "refreshToken:";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    // TTL 은 JWT exp 와 같은 설정에서 온다 -> 저장본이 먼저 사라져 재발급이 실패하는 창을 없앤다
    private final Duration refreshTtl;

    public TokenIssuer(
            JwtUtil jwtUtil,
            @Qualifier("authRedisTemplate") StringRedisTemplate redisTemplate,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpirationMillis
    ) {
        this(jwtUtil, redisTemplate, Duration.ofMillis(refreshTokenExpirationMillis));
    }

    // 테스트에서 TTL 을 직접 넘기기 위한 생성자
    TokenIssuer(JwtUtil jwtUtil, StringRedisTemplate redisTemplate, Duration refreshTtl) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
        this.refreshTtl = refreshTtl;
    }

    public void issue(HttpServletResponse response, UUID userId, Role role, String name) {
        String accessToken = jwtUtil.generateAccessToken(userId, role, name);
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        jwtUtil.setHttpOnlyAllToken(response, accessToken, refreshToken);

        // 발급과 저장이 한 메서드 안에 있어야 재발급 시 대조할 서버 보관본이 항상 존재한다
        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + userId,
                refreshToken,
                refreshTtl.toSeconds(),
                TimeUnit.SECONDS);

        log.info("[Auth] 토큰 발급 완료 - userId: {}, refreshTtl: {}", userId, refreshTtl);
    }
}
