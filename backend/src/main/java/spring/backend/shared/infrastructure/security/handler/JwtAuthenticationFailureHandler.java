package spring.backend.shared.infrastructure.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.format.ApiResponseFormat;
import spring.backend.shared.response.format.ErrorDetailFormat;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFailureHandler implements AuthenticationFailureHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void onAuthenticationFailure(
          HttpServletRequest request,
          HttpServletResponse response,
          AuthenticationException exception
  ) throws IOException {

    // ErrorCode 사용
    ErrorCode errorCode = ErrorCode.INVALID_CREDENTIALS;

    // ErrorDetailFormat 생성
    ErrorDetailFormat errorDetail = new ErrorDetailFormat(
            null,
            null,
            exception.getMessage(),
            errorCode.getCode()
    );

    // 공통 응답 포맷으로 래핑
    ApiResponseFormat<Void> apiResponse = ApiResponseFormat.error(
            errorCode.getStatus(),
            errorCode.getMessage(),
            errorDetail
    );

    // JSON 응답 반환
    response.setStatus(errorCode.getStatus());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), apiResponse);
  }
}
