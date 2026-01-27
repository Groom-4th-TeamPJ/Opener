package spring.backend.shared.infrastructure.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import spring.backend.domain.auth.model.entity.Credentials;
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

    // LockedException인 경우 (이미 잠긴 계정으로 시도)
    if (exception instanceof LockedException) {
      errorCode = ErrorCode.ACCOUNT_LOCKED;
      errorMessage = exception.getMessage();
    } else {
      // 일반 인증 실패 (비밀번호 틀림 등)
      // 로그인 실패 기록 후 잠금 상태 확인
      Optional<LockInfo> lockInfo = recordLoginFailureAndCheckLock(request);

      if (lockInfo.isPresent()) {
        // 이번 실패로 계정이 잠긴 경우
        errorCode = ErrorCode.ACCOUNT_LOCKED;
        errorMessage = String.format("계정이 잠겼습니다. %d분 후 다시 시도해주세요.",
                lockInfo.get().remainingMinutes());
      } else {
        errorCode = ErrorCode.INVALID_CREDENTIALS;
        errorMessage = errorCode.getMessage();
      }
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
   * 로그인 실패 기록 및 계정 잠금 상태 확인
   * @return 계정이 잠긴 경우 LockInfo 반환, 그렇지 않으면 empty
   */
  private Optional<LockInfo> recordLoginFailureAndCheckLock(HttpServletRequest request) {
    try {
      // Filter에서 저장한 request attribute에서 email 추출
      String email = (String) request.getAttribute("loginEmail");

      if (email == null || email.isBlank()) {
        return Optional.empty();
      }

      // Credentials 조회
      Optional<Credentials> credentialsOpt = credentialRepository.findUserCredentialByEmail(email);

      if (credentialsOpt.isEmpty()) {
        return Optional.empty();
      }

      Credentials credentials = credentialsOpt.get();

      // 로그인 실패 기록
      credentials.recordLoginFailure(
              securityProperties.getAccountLock().getMaxAttempts(),
              securityProperties.getAccountLock().getLockDurationMinutes()
      );

      // DB에 변경사항 저장
      credentialRepository.save(credentials);

      // 잠금 상태 확인 후 반환
      if (credentials.isAccountLocked()) {
        return Optional.of(new LockInfo(credentials.getRemainingLockTimeMinutes()));
      }

      return Optional.empty();
    } catch (Exception e) {
      // 로그인 실패 기록 중 예외 발생 시 무시 (로그인 자체는 진행)
      return Optional.empty();
    }
  }

  /**
   * 계정 잠금 정보
   */
  private record LockInfo(long remainingMinutes) {}
}
