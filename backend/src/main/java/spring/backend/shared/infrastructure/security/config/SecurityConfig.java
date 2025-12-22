package spring.backend.shared.infrastructure.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import spring.backend.shared.infrastructure.security.filter.JwtAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            // csrf 비활성화(JWT 사용)
            .csrf(csrf -> csrf.disable())

            // 엔드포인트 권한 설정
            .authorizeHttpRequests(auth -> auth

                    // Auth 관련 엔드포인트 (회원가입, 로그인, 토큰 갱신) - 인증 불필요
                    .requestMatchers("/v1/auth/**").permitAll()

                    // Swagger UI (application-dev.yml에서만 동작)
                    .requestMatchers(
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**"
                    ).permitAll()

                    // 나머지 모든 요청 - 인증 필요
                    .anyRequest().permitAll()
            )

            // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 이전에 추가
            // JWT 토큰 여부에 따라 로그인 프로세스 실행 여부 분기됨
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  // 비밀번호 암호화를 위한 PasswordEncoder, Bcrypt 사용
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}