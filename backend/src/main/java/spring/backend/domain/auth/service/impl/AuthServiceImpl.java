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

// 클래스 레벨 @Transactional -> 회원가입/토큰발급 등 다단계 쓰기를 한 단위로 묶어 부분 저장 방지
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

    // @Qualifier -> Redis 빈이 둘이라 인증 전용 standalone 템플릿을 명시 선택, chat 클러스터와 격리
    // RefreshToken/블랙리스트는 만료가 핵심이라 TTL 지원하는 Redis 사용 (DB 대비 조회도 빠름)
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

        // 남은 만료시간만큼만 TTL 부여 -> JWT 는 서버가 강제 폐기 못 하므로, 토큰 수명까지만 차단하면 충분
        long ttl = Math.max(
                (claim.getExpiration().getTime() - System.currentTimeMillis()) / 1000,
                0
        );

        // 블랙리스트 등록 -> stateless JWT 의 약점(로그아웃 후에도 토큰 유효) 보완, 필터에서 이 키 조회로 차단
        // TTL 자동 만료 -> 만료된 토큰 키가 Redis 에 영원히 쌓이지 않음
        redisTemplate.opsForValue()
                .set(
                        "blacklist:access:" + claim.getId(), // jti(토큰 고유 id) 기준 -> 동일 사용자 다른 토큰은 영향 없음
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

        // Redis 저장본과 대조 -> 서명만 검증하면 탈취/구버전 토큰도 통과하므로, 서버 보관본과 일치해야만 재발급
        // 키를 userId 단위로 둠 -> 재로그인 시 토큰 덮어써져 이전 RefreshToken 자동 무효화
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
