package spring.backend.domain.chat.service.spec;

import reactor.core.publisher.Flux;

public interface LlmService {

    // LLM API 스트리밍 호출 — Flux 반환으로 Non-blocking 소비 지원
    // writtenLength = userMessage 저장이 돌려준 목록 길이 -> 질문 직전까지만 히스토리로 읽는다
    Flux<String> chatStream(String sessionId, String userMessage, long writtenLength);

    // 채팅 기록 요약
    String summaryChat(String sessionId);
}
