package spring.backend.shared.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import spring.backend.shared.response.format.ApiResponseFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// 미매핑 경로가 포괄 Exception 핸들러로 떨어지면 클라이언트 오류가 5xx 로 집계된다
// 5xx 비율로 알림·서킷을 판단하는 구조에서는 오탈자 URL 이 장애 신호를 오염시킨다
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("미매핑 경로는 500 이 아니라 404 로 응답한다")
  void handleNoResourceFound_returns404() {
    ResponseEntity<ApiResponseFormat<Void>> response =
            handler.handleNoResourceFound(noResourceFound("/api/no-such-path"));

    assertEquals(404, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(404, response.getBody().getCode());
    assertEquals("error", response.getBody().getStatus());
  }

  // 서버 에러 코드(S_001) 가 붙으면 모니터링이 서버 장애로 집계한다
  @Test
  @DisplayName("미매핑 경로의 에러 코드는 서버 에러가 아닌 B_001 이다")
  void handleNoResourceFound_usesResourceNotFoundCode() {
    ResponseEntity<ApiResponseFormat<Void>> response =
            handler.handleNoResourceFound(noResourceFound("/api/no-such-path"));

    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getError());
    assertEquals("B_001", response.getBody().getError().getCode());
  }

  @Test
  @DisplayName("허용되지 않은 HTTP 메서드는 405 로 응답한다")
  void handleMethodNotSupported_returns405() {
    ResponseEntity<ApiResponseFormat<Void>> response =
            handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("DELETE"));

    assertEquals(405, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getError());
    assertEquals("V_003", response.getBody().getError().getCode());
  }

  // Spring Framework 7 생성자는 (HttpMethod, String, String) 이다
  // 두 문자열의 역할 구분이 테스트 의도와 무관하므로 양쪽에 같은 경로를 넣는다
  private NoResourceFoundException noResourceFound(String path) {
    return new NoResourceFoundException(HttpMethod.GET, path, path);
  }

  // 갈라낸 뒤에도 진짜 서버 예외는 여전히 500 / S_001 이어야 한다
  @Test
  @DisplayName("정체불명 예외는 그대로 500 으로 남는다")
  void handleException_stillReturns500() {
    ResponseEntity<ApiResponseFormat<Void>> response =
            handler.handleException(new IllegalStateException("예상치 못한 오류"));

    assertEquals(500, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getError());
    assertEquals("S_001", response.getBody().getError().getCode());
  }
}
