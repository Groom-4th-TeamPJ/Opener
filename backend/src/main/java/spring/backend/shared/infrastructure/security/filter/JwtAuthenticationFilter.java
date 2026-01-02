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
      String token = jwtUtil.extractTokenFormRequest(request);

      // 토큰 여부 확인 및 토큰 인증 진행
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

        SecurityContextHolder.getContext().setAuthentication(authentication);

      }

    } catch (Exception e) {
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);

  }
}
