package spring.backend.shared.response;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import spring.backend.shared.response.codes.SuccessCode;
import spring.backend.shared.response.format.ApiResponseFormat;

@Slf4j
@RestControllerAdvice
public class GlobalResponse implements ResponseBodyAdvice<Object> {

  @Override
  public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    log.info("=== supports() called ===");
    log.info("Return Type: {}", returnType.getParameterType());
    log.info("Converter Type: {}", converterType.getName());

    // ApiResponseFormat은 이미 래핑된 응답이므로 처리하지 않음
    boolean isApiResponse = ApiResponseFormat.class.isAssignableFrom(returnType.getParameterType());

    log.info("Is ApiResponseFormat: {}", isApiResponse);
    log.info("Should process: {}", !isApiResponse);

    return !isApiResponse;
  }

  @Override
  public Object beforeBodyWrite(
          Object body,
          MethodParameter returnType,
          MediaType selectedContentType,
          Class<? extends HttpMessageConverter<?>> selectedConverterType,
          ServerHttpRequest request,
          ServerHttpResponse response) {

    log.info("=== beforeBodyWrite() called ===");
    log.info("Request Path: {}", request.getURI().getPath());
    log.info("Body: {}", body);
    log.info("Content Type: {}", selectedContentType);

    // 이미 ApiResponseFormat이면 그대로 반환 (이중 검증)
    if (body instanceof ApiResponseFormat) {
      log.info("Already ApiResponseFormat, returning as is");
      return body;
    }

    // String 타입은 특별 처리 필요 (Jackson 변환 이슈)
    if (body instanceof String) {
      log.info("String type detected, wrapping in ApiResponseFormat");
    }

    // Servlet 환경에서 상태코드 추출
    HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
    int statusCode = servletResponse.getStatus();

    log.info("Status Code: {}", statusCode);

    // 상태코드에 해당하는 SuccessCode Enum 찾기
    SuccessCode successCode = SuccessCode.fromCode(statusCode);

    // 매칭되는 코드가 없으면 기본값 사용
    if (successCode == null) {
      log.warn("No matching SuccessCode for {}, using default OK", statusCode);
      successCode = SuccessCode.OK;
    }

    log.info("Success Code: {}, Message: {}", successCode, successCode.getMessage());

    // ApiResponseFormat으로 감싸기
    ApiResponseFormat<Object> result = ApiResponseFormat.success(
            statusCode,
            successCode.getMessage(),
            body
    );

    log.info("=== Response wrapped successfully ===");

    return result;
  }
}
