package spring.backend.shared.infrastructure.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import spring.backend.shared.infrastructure.security.filter.FormAuthenticationFilter;
import spring.backend.shared.infrastructure.security.filter.JwtAuthenticationFilter;
import spring.backend.shared.infrastructure.security.handler.FormAuthenticationFailureHandler;
import spring.backend.shared.infrastructure.security.handler.FormAuthenticationSuccessHandler;
import spring.backend.shared.infrastructure.security.handler.OAuth2AuthenticationSuccessHandler;
import spring.backend.shared.infrastructure.security.oauth2.CustomOAuth2UserService;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final FormAuthenticationSuccessHandler formAuthenticationSuccessHandler;
  private final FormAuthenticationFailureHandler formAuthenticationFailureHandler;
  private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
  private final CustomOAuth2UserService customOAuth2UserService;
  private final UserDetailsService userDetailsService;
  private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins:}")
    private String[] allowedOrigins;

  @Bean
  public SecurityFilterChain filterChain(
          HttpSecurity http,
          AuthenticationManager authenticationManager
  ) throws Exception {

    http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/auth/**",
                            "/login/oauth2/code/**"  // OAuth2 콜백 URL 허용
                    ).permitAll()
                    .anyRequest().authenticated()
            )

            // REST API용 예외 처리
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, e) ->
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
                    )
            )

            // OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                    )
                    .successHandler(oAuth2AuthenticationSuccessHandler)
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
    filter.setFilterProcessesUrl("/auth/form-login");

    // 성공/실패 핸들러 설정
    filter.setAuthenticationSuccessHandler(formAuthenticationSuccessHandler);
    filter.setAuthenticationFailureHandler(formAuthenticationFailureHandler);

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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if (allowedOrigins != null && allowedOrigins.length > 0) {
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
            configuration.setAllowCredentials(true);
        } else {
            // 운영 설정 누락 시 안전한 fallback (개발용). 운영에서는 사용 금지 권장.
            configuration.setAllowedOriginPatterns(List.of("*"));
            configuration.setAllowCredentials(false);
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}