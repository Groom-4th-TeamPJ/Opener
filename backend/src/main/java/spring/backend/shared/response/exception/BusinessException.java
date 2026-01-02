package spring.backend.shared.response.exception;

import lombok.Getter;
import spring.backend.shared.response.codes.ErrorCode;

@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Object data;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.data = null;
  }

  public BusinessException(ErrorCode errorCode, String customMessage) {
    super(customMessage);
    this.errorCode = errorCode;
    this.data = null;
  }

  public BusinessException(ErrorCode errorCode, Object data) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.data = data;
  }

  public BusinessException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
    this.data = null;
  }
}
