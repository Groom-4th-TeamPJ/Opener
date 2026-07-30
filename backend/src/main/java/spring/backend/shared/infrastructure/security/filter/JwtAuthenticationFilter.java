package spring.backend.shared.infrastructure.security.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import spring.backend.shared.infrastructure.security.dto.AuthUser;
import spring.backend.shared.infrastructure.security.util.JwtUtil;

// OncePerRequestFilter 상속 -> forward/include 로 요청이 재진입해도 인증을 딱 한 번만 수행
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  // 로그아웃이 기록하는 키와 동일 -> AuthServiceImpl 쓰기 / 이 필터 읽기로 짝을 이룸
  private static final String BLACKLIST_KEY_PREFIX = "blacklist:access:";

  private final JwtUtil jwtUtil;

  // 인증용 standalone Redis 를 명시 선택 -> chat 클러스터 빈도 StringRedisTemplate 이라 타입만으론 구분 불가
  private final StringRedisTemplate redisTemplate;

  public JwtAuthenticationFilter(
          JwtUtil jwtUtil,
          @Qualifier("authRedisTemplate") StringRedisTemplate redisTemplate
  ) {
    this.jwtUtil = jwtUtil;
    this.redisTemplate = redisTemplate;
  }

  @Override
  protected void doFilterInternal(
          HttpServletRequest request,
          HttpServletResponse response,
          FilterChain filterChain
  ) throws ServletException, IOException {

    try {
      // 토큰 추출
      String token = jwtUtil.extractAccessTokenFromRequest(request);

      // 토큰 없으면 인증 생략 -> 로그인/회원가입 등 공개 경로를 막지 않기 위함, 인가 판단은 뒤 단계가 담당
      if (token != null) {

        // 토큰 인증 - payload 가져오기
        Claims claims = jwtUtil.validateToken(token);

        // 서명·만료 검증 통과 후 폐기 여부 확인 -> stateless JWT 는 서버가 회수 못 하므로 블랙리스트로 보완
        if (isBlacklisted(claims.getId())) {

          // 인증 주입 생략 -> 로그아웃된 토큰은 미인증 취급, 실제 차단은 인가 단계가 401 로 처리
          SecurityContextHolder.clearContext();
          log.info("[Auth] 폐기된 토큰으로 접근 - userId: {}", claims.getSubject());

        } else {

          AuthUser userInfo = jwtUtil.getUserInfoFromToken(claims);

          UsernamePasswordAuthenticationToken authentication =
                  new UsernamePasswordAuthenticationToken(
                          userInfo,
                          null,// credentials (비밀번호는 불필요)
                          List.of(new SimpleGrantedAuthority("ROLE_" + claims.get("role", String.class)))
                  );

          authentication.setDetails(
                  new WebAuthenticationDetailsSource().buildDetails(request)
          );

          // SecurityContext 에 인증 주입 -> 이후 컨트롤러가 @AuthenticationPrincipal 로 사용자 사용 가능
          SecurityContextHolder.getContext().setAuthentication(authentication);
        }

      }

    } catch (Exception e) {
      // 검증 실패 시 막지 않고 컨텍스트만 비움 -> 미인증 상태로 진행, 보호 자원 접근은 인가 단계에서 401/403 차단
      SecurityContextHolder.clearContext();
    }

    // 항상 다음 필터로 진행 -> 인증 필터는 통과 여부만 정하고, 접근 거부는 인가 책임으로 분리
    filterChain.doFilter(request, response);

  }

  // 로그아웃으로 폐기된 access token 인지 확인
  private boolean isBlacklisted(String jti) {

    // jti 없는 토큰은 폐기 취급 -> jti 는 서명 대상이라 위조 불가, 따라서 없는 토큰은
    // (1) jti 도입 전 구버전 access token 이거나 (2) accessToken 쿠키에 끼워 넣은 refresh token 뿐
    // 둘 다 블랙리스트로 개별 폐기가 불가능해 통과시키면 우회 경로가 됨
    // access token 수명이 1시간이라 (1) 의 영향은 배포 후 최대 1시간 재로그인으로 끝남
    if (jti == null || jti.isBlank()) {
      log.info("[Auth] jti 없는 토큰 거부");
      return true;
    }

    try {
      return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));

    } catch (RuntimeException e) {
      // fail-close 선택 -> Redis 장애 시 인증을 막는다
      // 통과시키면(fail-open) 장애 구간 동안 폐기된 토큰이 그대로 살아나 로그아웃 자체가 무력화됨
      // 반대로 막으면 전체 사용자가 일시적으로 401 을 받지만, 인증 Redis 는 refresh 경로도 함께 쓰므로
      // 어차피 이 구간에서 정상 서비스가 어렵고, 복구 후 즉시 회복됨
      log.error("[Auth] 블랙리스트 조회 실패 - 인증 거부 (fail-close)", e);
      return true;
    }
  }
}
