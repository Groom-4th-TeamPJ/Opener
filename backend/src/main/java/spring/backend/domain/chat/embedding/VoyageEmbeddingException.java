package spring.backend.domain.chat.embedding;

// Voyage 호출이 끝내 실패한 경우의 전용 타입 -> 메시지에 HTTP 상태를 담고 API 키는 담지 않는다
public class VoyageEmbeddingException extends RuntimeException {

    public VoyageEmbeddingException(String message) {
        super(message);
    }

    public VoyageEmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
