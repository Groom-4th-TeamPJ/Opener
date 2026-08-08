package spring.backend.shared.response;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;
import spring.backend.shared.response.format.ApiResponseFormat;
import spring.backend.shared.response.format.ErrorDetailFormat;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * BusinessException 처리 (비즈니스 로직 예외)
   */
  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponseFormat<Void>> handleBusinessException(BusinessException e) {
    log.error("BusinessException occurred: {}", e.getMessage(), e);

    ErrorCode errorCode = e.getErrorCode();

    // 단일 error 객체 생성
    ErrorDetailFormat error = new ErrorDetailFormat(
            null,
            null,
            e.getMessage(),
            errorCode.getCode()
    );

    ApiResponseFormat<Void> response = ApiResponseFormat.error(
            errorCode.getStatus(),
            errorCode.getMessage(),
            error
    );

    return ResponseEntity
            .status(errorCode.getStatus())
            .body(response);
  }

  /**
   * 입력값 검증 실패 예외 처리 (Validation)
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponseFormat<Void>> handleValidationException(MethodArgumentNotValidException e) {
    log.error("Validation error occurred: {}", e.getMessage());

    // 첫 번째 검증 실패 필드 정보만 추출
    ErrorDetailFormat error;
    if (!e.getBindingResult().getFieldErrors().isEmpty()) {
      var fieldError = e.getBindingResult().getFieldErrors().get(0);
      error = new ErrorDetailFormat(
              fieldError.getField(),
              fieldError.getRejectedValue(),
              fieldError.getDefaultMessage(),
              "C_001"  // 클라이언트 입력 에러 코드
      );
    } else {
      error = new ErrorDetailFormat(
              null,
              null,
              e.getMessage(),
              "C_001"
      );
    }

    ApiResponseFormat<Void> response = ApiResponseFormat.error(
            400,
            "입력값이 올바르지 않습니다",
            error
    );

    return ResponseEntity
            .status(400)
            .body(response);
  }

  /**
   * 미매핑 경로 처리 (404)
   * 포괄 Exception 핸들러로 떨어지면 클라이언트 오류가 5xx 로 집계돼 모니터링 신호를 오염시킨다
   */
  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponseFormat<Void>> handleNoResourceFound(NoResourceFoundException e) {
    // 오탈자 URL 은 장애가 아니므로 warn 까지만 남긴다
    log.warn("No handler found for {} {}", e.getHttpMethod(), e.getResourcePath());

    return toErrorResponse(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.getMessage());
  }

  /**
   * 허용되지 않은 HTTP 메서드 처리 (405)
   */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponseFormat<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
    log.warn("Method not supported: {}", e.getMessage());

    return toErrorResponse(ErrorCode.METHOD_NOT_ALLOWED, e.getMessage());
  }

  /**
   * ErrorCode 기반 에러 응답 조립
   */
  private ResponseEntity<ApiResponseFormat<Void>> toErrorResponse(ErrorCode errorCode, String detailMessage) {
    ErrorDetailFormat error = new ErrorDetailFormat(
            null,
            null,
            detailMessage,
            errorCode.getCode()
    );

    return ResponseEntity
            .status(errorCode.getStatus())
            .body(ApiResponseFormat.error(
                    errorCode.getStatus(),
                    errorCode.getMessage(),
                    error
            ));
  }

  /**
   * 모든 예외의 기본 처리
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponseFormat<Void>> handleException(Exception e) {
    log.error("Unexpected error occurred: {}", e.getMessage(), e);

    ErrorDetailFormat error = new ErrorDetailFormat(
            null,
            null,
            e.getMessage(),
            "S_001"  // 서버 에러 코드
    );

    ApiResponseFormat<Void> response = ApiResponseFormat.error(
            500,
            "서버 내부 오류가 발생했습니다",
            error
    );

    return ResponseEntity
            .status(500)
            .body(response);
  }
}
