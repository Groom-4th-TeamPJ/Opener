package spring.backend.domain.chat.service.spec;

import reactor.core.publisher.Flux;

public interface LlmService {

    // LLM API 스트리밍 호출 — Flux 반환으로 Non-blocking 소비 지원
    Flux<String> chatStream(String sessionId, String userMessage);

    // 채팅 기록 요약
    String summaryChat(String sessionId);
}
