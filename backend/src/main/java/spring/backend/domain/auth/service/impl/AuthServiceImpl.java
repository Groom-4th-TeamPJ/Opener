package spring.backend.domain.auth.service.impl;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import spring.backend.domain.can.service.spec.CanService;
import spring.backend.domain.user.model.entity.User;
import spring.backend.domain.user.repository.spec.UserRepository;
import spring.backend.shared.infrastructure.security.dto.OAuthSignupInfo;
import spring.backend.shared.infrastructure.security.util.JwtUtil;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final CredentialRepository credentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JpaCredentialRepository jpaCredentialRepository;
    private final UserRepository userRepository;
    private final CanService canService;

    @Qualifier("authRedisTemplate")
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
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

        // 초기 캔설정
        canService.createUserCan(user.getId());

        // 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(
                newCredential.getUser().getId(),
                newCredential.getUser().getRole(),
                newCredential.getUser().getName());
        String refreshToken = jwtUtil.generateRefreshToken(newCredential.getUser().getId());

        jwtUtil.setHttpOnlyAllToken(response, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public void oAuthSignup(HttpServletResponse response, OAuthSignupRequest req) {
        // 1. signupToken 검증 및 OAuth 정보 추출
        OAuthSignupInfo oAuthInfo = jwtUtil.getOAuthInfoFromSignupToken(req.signupToken());

        // 2. 이미 가입된 사용자인지 확인
        if (credentialRepository.findUserCredentialByProviderId(oAuthInfo.providerId()).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_REGISTERED_USER);
        }

        // 3. User 생성 (프론트에서 받은 이름 사용)
        User user = User.createUser(req.name());

        // 4. OAuth Credentials 생성
        Credentials credential = Credentials.createOAuthCredentials(
                user,
                oAuthInfo.provider(),
                oAuthInfo.providerId()
        );

        // 5. 저장
        Credentials savedCredential = jpaCredentialRepository.save(credential);

        // 6. 초기 캔 설정
        canService.createUserCan(user.getId());

        // 7. JWT 토큰 생성
        String accessToken = jwtUtil.generateAccessToken(
                savedCredential.getUser().getId(),
                savedCredential.getUser().getRole(),
                savedCredential.getUser().getName()
        );
        String refreshToken = jwtUtil.generateRefreshToken(savedCredential.getUser().getId());

        // 8. HttpOnly 쿠키에 토큰 설정
        jwtUtil.setHttpOnlyAllToken(response, accessToken, refreshToken);
    }

    @Override
    public void logout(HttpServletRequest req, HttpServletResponse res) {

        String bearerToken = jwtUtil.extractAccessTokenFromRequest(req);

        Claims claim = jwtUtil.validateToken(bearerToken);

        long ttl = Math.max(
                (claim.getExpiration().getTime() - System.currentTimeMillis()) / 1000,
                0
        );

        // Redis에 access token 블랙리스트 추가
        redisTemplate.opsForValue()
                .set(
                        "blacklist:access:" + claim.getId(), // ⭐ get("jti") 말고 getId()
                        "logout",
                        ttl,
                        TimeUnit.SECONDS
                );

        // httpOnly 쿠키 삭제 (accessToken, refreshToken)
        jwtUtil.clearAllTokenCookies(res);

        log.info("[Auth] 로그아웃 완료 - userId: {}, 토큰 블랙리스트 등록 및 쿠키 삭제", claim.getSubject());
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
