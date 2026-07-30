package spring.backend.shared.infrastructure.security.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import spring.backend.shared.infrastructure.security.dto.AuthUser;
import spring.backend.shared.infrastructure.security.util.JwtUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// 블랙리스트 조회는 로그아웃을 실제로 강제하는 지점 -> 이 필터가 통과시키면 로그아웃이 무력화됨
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "dummy-access-token";
    private static final String JTI = "jti-1";

    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private Claims claims;
    @Mock private FilterChain filterChain;

    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtUtil, redisTemplate);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    // SecurityContext 는 ThreadLocal 이라 정리하지 않으면 다음 테스트로 인증이 새어 나감
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("블랙리스트에 있는 jti 면 인증을 주입하지 않는다")
    void blacklistedJti_noAuthentication() throws Exception {
        when(jwtUtil.extractAccessTokenFromRequest(request)).thenReturn(TOKEN);
        when(jwtUtil.validateToken(TOKEN)).thenReturn(claims);
        when(claims.getId()).thenReturn(JTI);
        when(redisTemplate.hasKey("blacklist:access:" + JTI)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // 인증 객체 자체를 만들지 않아야 함
        verify(jwtUtil, never()).getUserInfoFromToken(any());
        // 인증 필터는 통과 여부만 정하고 차단은 인가 단계 책임 -> 체인은 계속 진행
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("jti 가 null 이면 Redis 조회 없이 거부한다")
    void nullJti_rejectedWithoutRedisLookup() throws Exception {
        when(jwtUtil.extractAccessTokenFromRequest(request)).thenReturn(TOKEN);
        when(jwtUtil.validateToken(TOKEN)).thenReturn(claims);
        when(claims.getId()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // jti 가 없으면 블랙리스트로 개별 폐기가 불가능 -> 조회할 것도 없이 거부
        verifyNoInteractions(redisTemplate);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("블랙리스트에 없는 정상 토큰이면 인증을 주입한다")
    void validToken_injectsAuthentication() throws Exception {
        UUID userId = UUID.randomUUID();
        AuthUser authUser = new AuthUser(userId, "테스터");

        when(jwtUtil.extractAccessTokenFromRequest(request)).thenReturn(TOKEN);
        when(jwtUtil.validateToken(TOKEN)).thenReturn(claims);
        when(claims.getId()).thenReturn(JTI);
        when(claims.get("role", String.class)).thenReturn("USER");
        when(redisTemplate.hasKey("blacklist:access:" + JTI)).thenReturn(false);
        when(jwtUtil.getUserInfoFromToken(claims)).thenReturn(authUser);

        filter.doFilterInternal(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals(authUser, authentication.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Redis 조회가 실패하면 fail-close 로 인증을 거부한다")
    void redisFailure_failClose() throws Exception {
        when(jwtUtil.extractAccessTokenFromRequest(request)).thenReturn(TOKEN);
        when(jwtUtil.validateToken(TOKEN)).thenReturn(claims);
        when(claims.getId()).thenReturn(JTI);
        when(redisTemplate.hasKey("blacklist:access:" + JTI))
                .thenThrow(new IllegalStateException("Redis down"));

        filter.doFilterInternal(request, response, filterChain);

        // 통과시키면 장애 구간 동안 폐기된 토큰이 전부 되살아남
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
