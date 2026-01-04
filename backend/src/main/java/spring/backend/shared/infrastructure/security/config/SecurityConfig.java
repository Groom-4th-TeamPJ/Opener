package spring.backend.shared.infrastructure.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import spring.backend.shared.infrastructure.security.filter.FormAuthenticationFilter;
import spring.backend.shared.infrastructure.security.filter.JwtAuthenticationFilter;
import spring.backend.shared.infrastructure.security.handler.JwtAuthenticationFailureHandler;
import spring.backend.shared.infrastructure.security.handler.JwtAuthenticationSuccessHandler;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final JwtAuthenticationSuccessHandler jwtAuthenticationSuccessHandler;
  private final JwtAuthenticationFailureHandler jwtAuthenticationFailureHandler;
  private final UserDetailsService userDetailsService;
  private final ObjectMapper objectMapper;

  @Bean
  public SecurityFilterChain filterChain(
          HttpSecurity http,
          AuthenticationManager authenticationManager
  ) throws Exception {

    http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())

            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/api/auth/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            )

            // REST API용 예외 처리
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, e) ->
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
                    )
            )

            // JWT 필터 추가 (인증 필터보다 먼저 실행)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

            // JSON 로그인 필터 추가
            .addFilterAt(
                    formAuthenticationFilter(authenticationManager),
                    UsernamePasswordAuthenticationFilter.class
            );

    return http.build();
  }

  // ========================================= 필터 빈 선언부

  // form 로그인 필터
  @Bean
  public FormAuthenticationFilter formAuthenticationFilter(
          AuthenticationManager authenticationManager
  ) {

    FormAuthenticationFilter filter =
            new FormAuthenticationFilter(authenticationManager, objectMapper);

    // 로그인 처리 URL 설정
    filter.setFilterProcessesUrl("/api/auth/form-login");

    // 성공/실패 핸들러 설정
    filter.setAuthenticationSuccessHandler(jwtAuthenticationSuccessHandler);
    filter.setAuthenticationFailureHandler(jwtAuthenticationFailureHandler);

    return filter;
  }

  // ========================================= 필터 빈 선언부

  // 커스텀 인증기 사용 선언
  @Bean
  public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return new ProviderManager(provider);
  }

  // 비밀번호 암호화를 위한 PasswordEncoder, Bcrypt 사용
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}