package spring.backend.shared.infrastructure.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import spring.backend.domain.auth.respository.spec.CredentialRepository;
import spring.backend.shared.infrastructure.security.config.SecurityProperties;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.format.ApiResponseFormat;
import spring.backend.shared.response.format.ErrorDetailFormat;

@Component
@RequiredArgsConstructor
public class FormAuthenticationFailureHandler implements AuthenticationFailureHandler {

  private final ObjectMapper objectMapper;
  private final CredentialRepository credentialRepository;
  private final SecurityProperties securityProperties;

  @Override
  @Transactional
  public void onAuthenticationFailure(
          HttpServletRequest request,
          HttpServletResponse response,
          AuthenticationException exception
  ) throws IOException {

    ErrorCode errorCode;
    String errorMessage;

    // LockedException인 경우 (계정 잠금)
    if (exception instanceof LockedException) {
      errorCode = ErrorCode.ACCOUNT_LOCKED;
      errorMessage = exception.getMessage();
    } else {
      // 일반 인증 실패 (비밀번호 틀림 등)
      errorCode = ErrorCode.INVALID_CREDENTIALS;
      errorMessage = errorCode.getMessage();

      // 로그인 실패 기록 (사용자 계정이 존재하는 경우)
      recordLoginFailure(request);
    }

    // ErrorDetailFormat 생성
    ErrorDetailFormat errorDetail = new ErrorDetailFormat(
            null,
            null,
            errorMessage,
            errorCode.getCode()
    );

    // 공통 응답 포맷으로 래핑
    ApiResponseFormat<Void> apiResponse = ApiResponseFormat.error(
            errorCode.getStatus(),
            errorMessage,
            errorDetail
    );

    // JSON 응답 반환
    response.setStatus(errorCode.getStatus());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), apiResponse);
  }

  /**
   * 로그인 실패 기록 및 계정 잠금 처리
   */
  private void recordLoginFailure(HttpServletRequest request) {
    try {

      // Request body에서 email 추출
      String email = request.getParameter("email");

      if (email == null || email.isBlank()) {
        return;
      }

      // Credentials 조회
      credentialRepository.findUserCredentialByEmail(email)
              .ifPresent(credentials -> {
                // 로그인 실패 기록
                credentials.recordLoginFailure(
                        securityProperties.getAccountLock().getMaxAttempts(),
                        securityProperties.getAccountLock().getLockDurationMinutes()
                );

              });
    } catch (Exception e) {
    }
  }
}
