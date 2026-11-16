package spring.backend.domain.chat.service.impl;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import spring.backend.domain.chat.service.spec.ChatHistoryProvider;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.domain.chat.util.PromptLoader;
import spring.backend.shared.response.exception.BusinessException;
import spring.backend.domain.chat.util.ChatPromptAssembler;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * RAG가 비활성화되었을 때 사용하는 LLM 서비스 멀티턴 대화는 지원하지만 RAG는 사용하지 않음
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.rag",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true // RAG 설정이 없으면 이 구현체 사용
)
public class OpenAiLlmServiceWithoutRag implements LlmService {

    private final ChatClient.Builder chatClientBuilder;
    // 대화 이력 조회 -> 프롬프트용 최근 메시지(getHistoryBefore)만 읽는다
    private final ChatHistoryProvider chatHistoryProvider;
    private final PromptLoader promptLoader;

    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;

    @PostConstruct
    public void validateApiKeyBinding() {
        log.info("[LLM-NoRAG] ========== OpenAI API 키 바인딩 검증 ==========");

        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            log.error("[LLM-NoRAG] ❌ OPENAI_API_KEY가 설정되지 않았습니다!");
            log.error("[LLM-NoRAG] 환경변수 또는 application.yml의 spring.ai.openai.api-key를 확인하세요.");
        } else if (openaiApiKey.equals("${OPENAI_API_KEY}")) {
            log.error("[LLM-NoRAG] ❌ OPENAI_API_KEY 환경변수가 바인딩되지 않았습니다!");
            log.error("[LLM-NoRAG] .env 파일이 올바르게 로드되었는지 확인하세요.");
        } else if (!openaiApiKey.startsWith("sk-")) {
            log.warn("[LLM-NoRAG] ⚠️ API 키 형식이 일반적인 OpenAI 키와 다릅니다 (sk-로 시작하지 않음)");
            log.info("[LLM-NoRAG] API 키 앞 10자: {}...", openaiApiKey.substring(0, Math.min(10, openaiApiKey.length())));
        } else {
            log.info("[LLM-NoRAG] ✅ OpenAI API 키가 정상적으로 바인딩되었습니다.");
            log.info("[LLM-NoRAG] API 키 앞 10자: {}...", openaiApiKey.substring(0, Math.min(10, openaiApiKey.length())));
        }

        log.info("[LLM-NoRAG] RAG 비활성화 상태 - 멀티턴 대화만 지원");
        log.info("[LLM-NoRAG] ================================================");
    }

    @Override
    public Flux<String> chatStream(String sessionId, String userMessage, long writtenLength) {
        // defer -> 히스토리 조회를 구독 시점으로 미룬다
        // 조립 시점에 Redis 가 던지면 호출자의 doOnError 가 돌지 않아 세션이 PROCESSING 에 갇힌다
        return Flux.defer(() -> buildStream(sessionId, userMessage, writtenLength));
    }

    private Flux<String> buildStream(String sessionId, String userMessage, long writtenLength) {
        // 질문 직전까지만 읽는다 -> 현재 질문은 요청에서 받은 원문을 아래에서 한 번 붙인다
        List<Message> chatHistory = chatHistoryProvider.getHistoryBefore(sessionId, writtenLength);

        log.debug("[LLM-NoRAG] 대화 히스토리 조회 완료 - sessionId: {}, 메시지 수: {}",
                sessionId, chatHistory.size());

        String systemRulePrompt = promptLoader.loadChatRulePrompt();
        ChatClient chatClient = chatClientBuilder.build();

        log.debug("[LLM-NoRAG] RAG 비활성화 상태 - 규칙 시스템 프롬프트 + 멀티턴 대화 지원");

        return chatClient
                .prompt()
                .system(systemRulePrompt)
                // 히스토리는 질문 직전까지만 읽으므로 호출자가 넘긴 원문을 여기서 한 번 붙인다
                // 저장이 실패한 턴도 같은 경로라 프롬프트에서 질문이 통째로 사라지는 경로가 없다
                .messages(ChatPromptAssembler.withCurrentQuestion(chatHistory, userMessage))
                .stream()
                .content()
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .doOnError(error ->
                        log.error("[LLM-NoRAG] 스트리밍 중 에러 발생 - sessionId: {}", sessionId, error))
                .doOnComplete(() ->
                        log.debug("[LLM-NoRAG] 스트리밍 완료 - sessionId: {}", sessionId));
    }

    // 요약도 무보호 외부 호출이었다 -> 실패가 컨슈머 재시도로 증폭돼 LLM 부하를 키운다
    @Override
    @CircuitBreaker(name = "llm-summary", fallbackMethod = "summarizeFallback")
    public String summarize(List<Message> chatHistory) {
        try {
            log.debug("[LLM-NoRAG] 대화 요약 시작 - 메시지 수: {}", chatHistory.size());

            // 프롬프트 로드 (서술적 요약 안내)
            String summaryPrompt = promptLoader.buildChatSummaryPrompt();

            // Spring AI ChatClient를 사용한 전체 응답
            ChatClient chatClient = chatClientBuilder.build();

            String summary = chatClient
                    .prompt()
                    .system(summaryPrompt)
                    .messages(chatHistory)
                    .call()
                    .content();

            log.info("[LLM-NoRAG] 대화 요약 완료 - 요약 길이: {}", summary != null ? summary.length() : 0);

            return summary;

        } catch (Exception e) {
            // 상세 예외 정보 로깅
            String errorType = e.getClass().getSimpleName();
            String errorMessage = e.getMessage();
            Throwable rootCause = e.getCause();
            String rootCauseMessage = rootCause != null ? rootCause.getMessage() : "없음";

            log.error("[LLM-NoRAG] 대화 요약 실패 - 예외타입: {}, 메시지: {}, 원인: {}",
                    errorType, errorMessage, rootCauseMessage);
            log.error("[LLM-NoRAG] 상세 스택트레이스:", e);

            // API 키 관련 오류 감지
            if (errorMessage != null && (
                    errorMessage.contains("API key") ||
                    errorMessage.contains("api_key") ||
                    errorMessage.contains("Unauthorized") ||
                    errorMessage.contains("401") ||
                    errorMessage.contains("authentication"))) {
                log.error("[LLM-NoRAG] ⚠️ API 키 문제 의심 - OPENAI_API_KEY 환경변수를 확인하세요!");
            }

            // 도메인 거절은 그대로 올리고 호출 실패만 전용 타입으로 던진다
            // BusinessException 으로 감싸면 llm-summary 의 ignoreExceptions 에 걸려 서킷이 실패를 세지 못한다
            if (e instanceof BusinessException be) {
                throw be;
            }
            throw new LlmUpstreamException("대화 요약 LLM 호출 실패", e);
        }
    }

    // 요약이 없어도 저장된 대화는 그대로다 -> 호출 실패는 null 로 돌려 요약 소비자가 재시도하게 한다
    @SuppressWarnings("unused")
    private String summarizeFallback(List<Message> chatHistory, Throwable t) {
        // 도메인 거절은 요약 누락으로 덮지 않는다 -> 호출 실패와 서킷 OPEN 만 null 로 흡수
        if (t instanceof BusinessException be) {
            throw be;
        }
        log.warn("[LLM-NoRAG] 요약 실패 - cause: {}", t.getMessage());
        return null;
    }
}
