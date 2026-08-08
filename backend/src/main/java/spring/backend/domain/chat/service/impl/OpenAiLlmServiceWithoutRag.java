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
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;
import spring.backend.domain.chat.util.ChatPromptAssembler;

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
    // 대화 이력 조회 -> 프롬프트용(getRecentHistory)과 요약용(getFullHistory)을 메서드로 구분
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
            log.info("[LLM-NoRAG] API 키 앞 10자: {}...", openaiApiKey.substring(0, 10));
        }

        log.info("[LLM-NoRAG] RAG 비활성화 상태 - 멀티턴 대화만 지원");
        log.info("[LLM-NoRAG] ================================================");
    }

    @Override
    public Flux<String> chatStream(String sessionId, String userMessage) {
        List<Message> chatHistory = chatHistoryProvider.getRecentHistory(sessionId);

        log.debug("[LLM-NoRAG] 대화 히스토리 조회 완료 - sessionId: {}, 메시지 수: {}",
                sessionId, chatHistory.size());

        String systemRulePrompt = promptLoader.loadChatRulePrompt();
        ChatClient chatClient = chatClientBuilder.build();

        log.debug("[LLM-NoRAG] RAG 비활성화 상태 - 규칙 시스템 프롬프트 + 멀티턴 대화 지원");

        return chatClient
                .prompt()
                .system(systemRulePrompt)
                // 히스토리는 replica 에서 읽으므로 방금 저장한 질문이 빠질 수 있다
                // 호출자가 넘긴 원문을 여기서 보장해야 프롬프트에서 질문이 통째로 사라지는 경로가 닫힌다
                .messages(ChatPromptAssembler.withCurrentQuestion(chatHistory, userMessage))
                .stream()
                .content()
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .doOnError(error ->
                        log.error("[LLM-NoRAG] 스트리밍 중 에러 발생 - sessionId: {}", sessionId, error))
                .doOnComplete(() ->
                        log.debug("[LLM-NoRAG] 스트리밍 완료 - sessionId: {}", sessionId));
    }

    @Override
    public String summaryChat(String sessionId) {
        try {
            // 윈도우 미적용 전량 조회 -> 이 요약은 PostgreSQL 에 영구 저장되므로 최근 N 턴만 보면 기록이 영구 손실
            List<Message> chatHistory = chatHistoryProvider.getFullHistory(sessionId);

            log.debug("[LLM-NoRAG] 대화 요약 시작 - sessionId: {}, 메시지 수: {}",
                    sessionId, chatHistory.size());

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

            log.info("[LLM-NoRAG] 대화 요약 완료 - sessionId: {}, 요약 길이: {}",
                    sessionId, summary != null ? summary.length() : 0);

            return summary;

        } catch (Exception e) {
            // 상세 예외 정보 로깅
            String errorType = e.getClass().getSimpleName();
            String errorMessage = e.getMessage();
            Throwable rootCause = e.getCause();
            String rootCauseMessage = rootCause != null ? rootCause.getMessage() : "없음";

            log.error("[LLM-NoRAG] 대화 요약 실패 - sessionId: {}, 예외타입: {}, 메시지: {}, 원인: {}",
                    sessionId, errorType, errorMessage, rootCauseMessage);
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

            throw new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);
        }
    }
}
