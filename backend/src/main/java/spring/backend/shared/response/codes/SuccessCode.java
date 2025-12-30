package spring.backend.shared.response.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessCode {

  // 2xx Success
  OK(200, "요청이 정상 처리되었습니다"),
  CREATED(201, "리소스가 생성되었습니다"),
  ACCEPTED(202, "요청이 수락되었습니다"),
  NO_CONTENT(204, "리소스가 삭제되었습니다");


  private final int code;
  private final String message;

  public static SuccessCode fromCode(int code) {
    for (SuccessCode status : values()) {
      if (status.code == code) {
        return status;
      }
    }
    return null;
  }
}
