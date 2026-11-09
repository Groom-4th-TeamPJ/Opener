package spring.backend.domain.chat.service.impl;

// OpenAI 호출 자체가 실패한 경우의 전용 타입
// BusinessException 은 llm-* 서킷의 ignoreExceptions 에 있어 집계되지 않으므로, 서킷이 세어야 하는 실패는 이 타입으로 던진다
public class LlmUpstreamException extends RuntimeException {

    public LlmUpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
