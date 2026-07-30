package spring.backend.domain.auth.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import spring.backend.domain.auth.respository.jpa.JpaCredentialRepository;
import spring.backend.domain.auth.respository.spec.CredentialRepository;
import spring.backend.domain.can.service.spec.CanService;
import spring.backend.domain.user.repository.spec.UserRepository;
import spring.backend.shared.infrastructure.security.util.JwtUtil;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// 로그아웃은 access 블랙리스트만으로는 부족 -> refreshToken 이 남으면 재발급으로 세션이 되살아남
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private CredentialRepository credentialRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private JpaCredentialRepository jpaCredentialRepository;
    @Mock private UserRepository userRepository;
    @Mock private CanService canService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private Claims claims;

    @InjectMocks
    private AuthServiceImpl service;

    @Test
    @DisplayName("로그아웃 시 refreshToken:{userId} 키를 삭제한다")
    void logout_deletesRefreshToken() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        UUID userId = UUID.randomUUID();

        when(jwtUtil.extractAccessTokenFromRequest(req)).thenReturn("access-token");
        when(jwtUtil.validateToken("access-token")).thenReturn(claims);
        when(claims.getId()).thenReturn("jti-1");
        when(claims.getSubject()).thenReturn(userId.toString());
        // 남은 수명이 있어야 블랙리스트 등록 경로를 탐
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000));
        doReturn(valueOperations).when(redisTemplate).opsForValue();

        service.logout(req, res);

        verify(valueOperations).set(eq("blacklist:access:jti-1"), eq("logout"), anyLong(), eq(TimeUnit.SECONDS));
        verify(redisTemplate).delete("refreshToken:" + userId);
        verify(jwtUtil).clearAllTokenCookies(res);
    }

    @Test
    @DisplayName("토큰이 없으면 예외 없이 쿠키만 정리하고 반환한다")
    void logout_noToken_clearsCookiesOnly() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        when(jwtUtil.extractAccessTokenFromRequest(req)).thenReturn(null);

        // 이미 로그아웃된 상태 = 목적 달성 -> 오류가 아님 (멱등성)
        assertDoesNotThrow(() -> service.logout(req, res));

        verify(jwtUtil).clearAllTokenCookies(res);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("위조 토큰이면 예외 없이 쿠키만 정리하고 반환한다")
    void logout_invalidToken_clearsCookiesOnly() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        when(jwtUtil.extractAccessTokenFromRequest(req)).thenReturn("forged");
        when(jwtUtil.validateToken("forged")).thenThrow(new JwtException("서명 불일치"));

        assertDoesNotThrow(() -> service.logout(req, res));

        // 우리가 발급한 토큰이 아니므로 폐기할 대상도 없음
        verify(jwtUtil).clearAllTokenCookies(res);
        verifyNoInteractions(redisTemplate);
    }
}
