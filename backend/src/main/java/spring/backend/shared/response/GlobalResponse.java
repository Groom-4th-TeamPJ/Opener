package spring.backend.shared.response;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.backend.shared.response.codes.SuccessCode;
import spring.backend.shared.response.format.ApiResponseFormat;

@RestControllerAdvice
public class GlobalResponse implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // ApiResponseFormat은 이미 래핑된 응답이므로 처리하지 않음
        if (ApiResponseFormat.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }

        // SseEmitter는 래핑하지 않음 (SSE 스트리밍 유지)
        if (SseEmitter.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }

        // Spring Boot Actuator 관련 응답은 래핑하지 않음
        String cls = returnType.getContainingClass().getName();
        if (cls.startsWith("org.springframework.boot.actuate") ||
                cls.startsWith("org.springframework.boot.webmvc.actuate")) {
            return false;
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        // 이미 ApiResponseFormat이면 그대로 반환 (이중 검증)
        if (body instanceof ApiResponseFormat) {
            return body;
        }

        String path = request.getURI().getPath();
        // Actuator 경로는 래핑하지 않음 (이중 검증)
        if (path.startsWith("/actuator") || path.startsWith("/actuator/")) {
            return body;
        }

        // Servlet 환경에서 상태코드 추출
        HttpServletResponse servletResponse = ((ServletServerHttpResponse) response).getServletResponse();
        int statusCode = servletResponse.getStatus();

        // 상태코드에 해당하는 SuccessCode Enum 찾기
        SuccessCode successCode = SuccessCode.fromCode(statusCode);

        // 매칭되는 코드가 없으면 기본값 사용
        if (successCode == null) {
            successCode = SuccessCode.OK;
        }

        // ApiResponseFormat으로 감싸기
        return ApiResponseFormat.success(
                statusCode,
                successCode.getMessage(),
                body
        );
    }
}
