package spring.backend.shared.infrastructure.security.service;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import spring.backend.domain.user.model.enums.Role;
import spring.backend.shared.infrastructure.security.util.JwtUtil;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// 발급 경로가 4개인데 Redis 저장이 1곳에만 있어 카카오·회원가입 사용자는 재발급이 항상 실패했다
// 발급과 저장을 한 메서드로 묶어 누락이 구조적으로 불가능해지는지 고정한다
@ExtendWith(MockitoExtension.class)
class TokenIssuerTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Duration REFRESH_TTL = Duration.ofDays(14);

    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private HttpServletResponse response;

    @Test
    @DisplayName("발급하면 쿠키 설정과 Redis 저장이 함께 일어난다")
    void issue_토큰발급_쿠키와레디스에모두반영된다() {
        when(jwtUtil.generateAccessToken(USER_ID, Role.USER, "테스터")).thenReturn("access");
        when(jwtUtil.generateRefreshToken(USER_ID)).thenReturn("refresh");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        TokenIssuer issuer = new TokenIssuer(jwtUtil, redisTemplate, REFRESH_TTL);

        issuer.issue(response, USER_ID, Role.USER, "테스터");

        verify(jwtUtil).setHttpOnlyAllToken(response, "access", "refresh");
        // 저장이 빠지면 재발급이 Redis 대조에서 항상 불일치가 되어 실패한다
        verify(valueOperations).set(
                eq("refreshToken:" + USER_ID),
                eq("refresh"),
                eq(REFRESH_TTL.toSeconds()),
                eq(TimeUnit.SECONDS));
    }
}
