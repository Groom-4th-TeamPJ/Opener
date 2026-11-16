package spring.backend.domain.chat.service.spec;

import java.util.List;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

public interface LlmService {

    // LLM API 스트리밍 호출 — Flux 반환으로 Non-blocking 소비 지원
    // writtenLength = userMessage 저장이 돌려준 목록 길이 -> 질문 직전까지만 히스토리로 읽는다
    Flux<String> chatStream(String sessionId, String userMessage, long writtenLength);

    // 저장된 대화 요약 -> 호출부가 메시지를 넘긴다
    // 저장 뒤에 요약하므로 Redis 버퍼는 이미 지워졌고, 실패하면 null(서킷 OPEN 포함)
    String summarize(List<Message> messages);
}
