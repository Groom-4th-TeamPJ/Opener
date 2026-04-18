package spring.backend.domain.chat.service.spec;

import reactor.core.publisher.Flux;

public interface RagService {
    // 유사 문제 생성 스트리밍 — Flux 반환으로 Non-blocking 소비 지원
    Flux<String> generateSimilarProblemStream(String problemContext);
}
