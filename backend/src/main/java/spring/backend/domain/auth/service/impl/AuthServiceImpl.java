package spring.backend.domain.auth.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
import spring.backend.shared.infrastructure.security.service.TokenIssuer;

// 클래스 레벨 @Transactional -> 회원가입/토큰발급 등 다단계 쓰기를 한 단위로 묶어 부분 저장 방지
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

  // 발급의 유일한 진입점 -> 경로마다 복제하면 저장 누락이 조용히 생긴다
  private final TokenIssuer tokenIssuer;

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

        tokenIssuer.issue(
                response,
                newCredential.getUser().getId(),
                newCredential.getUser().getRole(),
                newCredential.getUser().getName());
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

        tokenIssuer.issue(
                response,
                savedCredential.getUser().getId(),
                savedCredential.getUser().getRole(),
                savedCredential.getUser().getName());
    }

    @Override
    public void logout(HttpServletRequest req, HttpServletResponse res) {

        String bearerToken = jwtUtil.extractAccessTokenFromRequest(req);

        Claims claim = parseForLogout(bearerToken);

        // 토큰 없음/위조 -> 이미 로그아웃된 상태로 보고 쿠키만 정리 (멱등성)
        // 로그아웃은 "세션이 없는 상태"를 만드는 요청이라, 이미 그 상태면 목적이 달성된 것이므로 오류가 아님
        // 예외를 던지면 500 과 함께 라이브러리 내부 메시지가 응답에 노출되고, 클라이언트는 재시도할 방법도 없음
        if (claim == null) {
            jwtUtil.clearAllTokenCookies(res);
            log.info("[Auth] 로그아웃 - 유효한 accessToken 없음, 쿠키만 정리");
            return;
        }

        // 남은 만료시간만큼만 TTL 부여 -> JWT 는 서버가 강제 폐기 못 하므로, 토큰 수명까지만 차단하면 충분
        long ttl = Math.max(
                (claim.getExpiration().getTime() - System.currentTimeMillis()) / 1000,
                0
        );

        // 이미 만료된 토큰은 등록 생략 -> 필터가 서명 검증 단계에서 거르므로 불필요하고, TTL 0 은 Redis 에서 에러
        if (ttl > 0) {
            // 블랙리스트 등록 -> stateless JWT 의 약점(로그아웃 후에도 토큰 유효) 보완, 필터에서 이 키 조회로 차단
            // TTL 자동 만료 -> 만료된 토큰 키가 Redis 에 영원히 쌓이지 않음
            redisTemplate.opsForValue()
                    .set(
                            "blacklist:access:" + claim.getId(), // jti(토큰 고유 id) 기준 -> 동일 사용자 다른 토큰은 영향 없음
                            "logout",
                            ttl,
                            TimeUnit.SECONDS
                    );
        }

        // 서버 보관 RefreshToken 삭제 -> 이게 없으면 남은 refreshToken 쿠키로 새 accessToken 을 계속 재발급받을 수 있음
        // access 블랙리스트만으로는 재발급 경로가 열려 있어 로그아웃이 사실상 무효
        redisTemplate.delete("refreshToken:" + claim.getSubject());

        // httpOnly 쿠키 삭제 (accessToken, refreshToken)
        jwtUtil.clearAllTokenCookies(res);

        log.info("[Auth] 로그아웃 완료 - userId: {}, 토큰 블랙리스트 등록 및 RefreshToken 폐기", claim.getSubject());
    }

    // 로그아웃 전용 토큰 파싱 -> 실패를 예외가 아닌 null 로 돌려 정상 흐름에서 처리
    private Claims parseForLogout(String token) {

        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            return jwtUtil.validateToken(token);

        } catch (ExpiredJwtException e) {
            // 만료 토큰도 payload 는 서명이 검증된 값 -> userId 를 꺼내 RefreshToken 폐기까지 마무리
            // 여기서 null 로 처리하면 만료 직후 로그아웃한 사용자의 refreshToken 이 최대 7일간 살아남음
            return e.getClaims();

        } catch (JwtException | IllegalArgumentException e) {
            // 서명 불일치/형식 오류 -> 우리가 발급한 토큰이 아니므로 폐기할 대상도 없음, 원인만 남기고 진행
            log.info("[Auth] 로그아웃 - 토큰 검증 실패({})", e.getClass().getSimpleName());
            return null;
        }
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
