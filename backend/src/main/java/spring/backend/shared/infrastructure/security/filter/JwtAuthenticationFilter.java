package spring.backend.shared.infrastructure.security.filter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import spring.backend.shared.infrastructure.security.dto.AuthUser;
import spring.backend.shared.infrastructure.security.util.JwtUtil;

// OncePerRequestFilter 상속 -> forward/include 로 요청이 재진입해도 인증을 딱 한 번만 수행
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtUtil jwtUtil;

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

    } catch (Exception e) {
      // 검증 실패 시 막지 않고 컨텍스트만 비움 -> 미인증 상태로 진행, 보호 자원 접근은 인가 단계에서 401/403 차단
      SecurityContextHolder.clearContext();
    }

    // 항상 다음 필터로 진행 -> 인증 필터는 통과 여부만 정하고, 접근 거부는 인가 책임으로 분리
    filterChain.doFilter(request, response);

  }
}
