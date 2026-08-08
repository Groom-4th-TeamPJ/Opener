package spring.backend.domain.chat.dto.request;

// 세션에 종속된 요청 DTO 공통 계약 -> 어드바이스가 리플렉션 없이 sessionId 를 얻는다
public interface SessionScoped {

    Long sessionId();
}
