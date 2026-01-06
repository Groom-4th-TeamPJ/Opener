package spring.backend.shared.response.format;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiResponseFormat<T> {
  private String status;
  private int code;
  private String message;
  private T data;
  private ErrorDetailFormat error;

  // 성공 응답
  public static <T> ApiResponseFormat<T> success(int code, String message, T data) {
    ApiResponseFormat<T> response = new ApiResponseFormat<>();
    response.status = "success";
    response.code = code;
    response.message = message;
    response.data = data;
    response.error = null;
    return response;
  }

  // 성공 응답 (data 없이)
  public static <T> ApiResponseFormat<T> success(int code, String message) {
    ApiResponseFormat<T> response = new ApiResponseFormat<>();
    response.status = "success";
    response.code = code;
    response.message = message;
    response.data = null;
    response.error = null;
    return response;
  }

  // 실패 응답 (단일 error 객체)
  public static <T> ApiResponseFormat<T> error(int code, String message, ErrorDetailFormat error) {
    ApiResponseFormat<T> response = new ApiResponseFormat<>();
    response.status = "error";
    response.code = code;
    response.message = message;
    response.data = null;
    response.error = error;
    return response;
  }
}
