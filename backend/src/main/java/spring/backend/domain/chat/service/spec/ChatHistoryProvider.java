package spring.backend.domain.chat.service.spec;

import java.util.List;
import org.springframework.ai.chat.messages.Message;

// 대화 이력 조회 계약 -> LLM 프롬프트용 최근 메시지 읽기만 제공한다
// 요약은 저장된 행의 메시지를 읽으므로 이 계약을 거치지 않는다
//
// Spring AI 의 ChatMemory 를 구현하지 않는 이유
// - ChatMemory 는 add/get/clear 를 한 묶음으로 요구하는데 우리는 get 만 쓴다
// - 쓰기는 ChatServiceImpl 이 전송 시점과 doOnComplete 에서 chatRedisService.saveMessage 로 직접 한다
// - 여기에 MessageChatMemoryAdvisor 를 등록하면 어드바이저가 같은 메시지를 또 add 해 중복 저장된다
// - 그래서 인터페이스만 구현해두고 어드바이저를 안 붙인 상태였고, add/clear 가 호출되지 않는 死 코드로 남아 있었다
public interface ChatHistoryProvider {

    // 방금 저장한 질문 직전까지의 최근 메시지만 조회 (app.chat.history-window) — LLM 프롬프트 컨텍스트 전용
    // 전량을 실으면 턴이 쌓일수록 프롬프트가 선형 증가해 비용과 첫 토큰 지연이 함께 나빠진다
    // writtenLength = 질문 저장이 돌려준 목록 길이, 음수면 저장이 버려진 턴
    List<Message> getHistoryBefore(String conversationId, long writtenLength);
}
