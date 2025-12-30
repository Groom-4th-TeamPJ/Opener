package spring.backend.domain.user.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@Slf4j
@RestController
@RequestMapping("/api")
public class UserController {

  // 간단한 응답 DTO
  public static class TestResponse {
    private String message;
    private Long timestamp;

    public TestResponse(String message) {
      this.message = message;
      this.timestamp = System.currentTimeMillis();
    }

    public String getMessage() {
      return message;
    }

    public Long getTimestamp() {
      return timestamp;
    }
  }

  @GetMapping("/test")
  public TestResponse test() {
    return new TestResponse("test");
  }

  @GetMapping("/test/error")
  public TestResponse testError() {
    throw new BusinessException(ErrorCode.USER_NOT_FOUND);
  }

  @GetMapping("/test/error/custom")
  public TestResponse testCustomError() {
    throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "커스텀 에러 메시지입니다");
  }

  @GetMapping("/test/error/server")
  public TestResponse testServerError() {
    throw new RuntimeException("예상치 못한 서버 에러 발생");
  }
}
