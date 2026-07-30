package spring.backend.domain.chat.service.spec;

import java.util.List;
import org.springframework.ai.chat.messages.Message;

// 대화 이력 조회 계약 -> 조회 "범위"가 소비자마다 다르다는 사실을 타입이 아니라 메서드로 드러냄
// 같은 메서드를 공유하면 한쪽 요구(윈도우)를 만족시키는 순간 다른 쪽(요약)이 조용히 깨진다
//
// Spring AI 의 ChatMemory 를 구현하지 않는 이유
// - ChatMemory 는 add/get/clear 를 한 묶음으로 요구하는데 우리는 get 만 쓴다
// - 쓰기는 ChatServiceImpl 이 전송 시점과 doOnComplete 에서 chatRedisService.saveMessage 로 직접 한다
// - 여기에 MessageChatMemoryAdvisor 를 등록하면 어드바이저가 같은 메시지를 또 add 해 중복 저장된다
// - 그래서 인터페이스만 구현해두고 어드바이저를 안 붙인 상태였고, add/clear 가 호출되지 않는 死 코드로 남아 있었다
public interface ChatHistoryProvider {

    // 최근 N 턴만 조회 (app.chat.history-window) — LLM 프롬프트 컨텍스트 전용
    // 전량을 실으면 턴이 쌓일수록 프롬프트가 선형 증가해 비용과 첫 토큰 지연이 함께 나빠진다
    List<Message> getRecentHistory(String conversationId);

    // 전체 대화 이력 조회 — 요약/영속화 전용
    // 요약은 PostgreSQL 에 영구 저장되므로 윈도우로 자르면 대화 기록이 영구히 반쪽이 된다
    List<Message> getFullHistory(String conversationId);
}
