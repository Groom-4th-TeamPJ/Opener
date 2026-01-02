package spring.backend.shared.infrastructure.security.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import spring.backend.shared.infrastructure.security.filter.JwtAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())

            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                            "/api/form-login",
                            "/api/oauth-login/**",
                            "/api/oauth2/**"
                    ).permitAll()
                    .anyRequest().authenticated()
            )

            // Form Login
            .formLogin(form -> form
                    .loginProcessingUrl("/api/form-login")
            )

//            // OAuth Login
//            .oauth2Login(oauth -> oauth
//                    .userInfoEndpoint(userInfo ->
//                            userInfo.userService(oAuth2UserService())
//                    )
//            )

            // REST API용
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, e) ->
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED)
                    )
            )

            // JWT 필터추가(form 인증 이전 추가)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  // 비밀번호 암호화를 위한 PasswordEncoder, Bcrypt 사용
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}