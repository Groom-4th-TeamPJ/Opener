package spring.backend.shared.infrastructure.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import spring.backend.domain.auth.dto.request.FormLoginRequest;

public class FormAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

  private final ObjectMapper objectMapper;

  public FormAuthenticationFilter(
          AuthenticationManager authenticationManager,
          ObjectMapper objectMapper
  ) {
    super(authenticationManager);
    this.objectMapper = objectMapper;
  }

  @Override
  public Authentication attemptAuthentication(
          HttpServletRequest request,
          HttpServletResponse response
  ) throws AuthenticationException {

    try {
      // JSON 요청 파싱
      FormLoginRequest loginRequest = objectMapper.readValue(
              request.getInputStream(),
              FormLoginRequest.class
      );

      // UsernamePasswordAuthenticationToken 생성
      UsernamePasswordAuthenticationToken authToken =
              new UsernamePasswordAuthenticationToken(
                      loginRequest.email(),
                      loginRequest.password()
              );

      // AuthenticationManager에 인증 위임
      return this.getAuthenticationManager().authenticate(authToken);

    } catch (IOException e) {
      throw new RuntimeException("로그인에 실패하였습니다.", e);
    }
  }
}
