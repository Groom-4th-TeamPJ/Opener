package spring.backend.domain.chat.service.impl;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import spring.backend.domain.chat.service.spec.ChatHistoryProvider;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

// @ConditionalOnProperty -> RAG 켜질 때만 이 구현 활성, 꺼지면 OpenAiLlmServiceWithoutRag 가 대신 주입
// 같은 LlmService 인터페이스를 RAG 유무로 갈아끼움 -> 호출부(ChatServiceImpl) 코드는 변경 불필요
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.rag",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class OpenAiLlmService implements LlmService {

    private final ChatClient.Builder chatClientBuilder;
    // 대화 이력 조회 -> 프롬프트용(getRecentHistory)과 요약용(getFullHistory)을 메서드로 구분
    private final ChatHistoryProvider chatHistoryProvider;
    private final VectorStore vectorStore; // RAG 검색용 -> 시험 문제 맥락에 맞는 근거 자료 확보
    private final spring.backend.domain.chat.util.PromptLoader promptLoader;

    @Value("${app.rag.top-k:5}")
    private int topK;

    @Value("${app.rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;

    // @PostConstruct -> 빈 생성 직후 API 키 바인딩 검증, 첫 요청 때 실패하지 않고 기동 시점에 조기 발견
    @PostConstruct
    public void validateApiKeyBinding() {
        log.info("[LLM+RAG] ========== OpenAI API 키 바인딩 검증 ==========");

        if (openaiApiKey == null || openaiApiKey.isEmpty()) {
            log.error("[LLM+RAG] ❌ OPENAI_API_KEY가 설정되지 않았습니다!");
            log.error("[LLM+RAG] 환경변수 또는 application.yml의 spring.ai.openai.api-key를 확인하세요.");
        } else if (openaiApiKey.equals("${OPENAI_API_KEY}")) {
            log.error("[LLM+RAG] ❌ OPENAI_API_KEY 환경변수가 바인딩되지 않았습니다!");
            log.error("[LLM+RAG] .env 파일이 올바르게 로드되었는지 확인하세요.");
        } else if (!openaiApiKey.startsWith("sk-")) {
            log.warn("[LLM+RAG] ⚠️ API 키 형식이 일반적인 OpenAI 키와 다릅니다 (sk-로 시작하지 않음)");
            log.info("[LLM+RAG] API 키 앞 10자: {}...", openaiApiKey.substring(0, Math.min(10, openaiApiKey.length())));
        } else {
            log.info("[LLM+RAG] ✅ OpenAI API 키가 정상적으로 바인딩되었습니다.");
            log.info("[LLM+RAG] API 키 앞 10자: {}...", openaiApiKey.substring(0, 10));
        }

        log.info("[LLM+RAG] RAG 설정 - topK: {}, similarityThreshold: {}", topK, similarityThreshold);
        log.info("[LLM+RAG] ================================================");
    }

    // @CircuitBreaker -> OpenAI 장애 누적 시 호출 차단, 느린 외부 API 가 SSE 스레드를 잡아 전체 지연되는 것 방지
    @Override
    @CircuitBreaker(name = "llm-chat", fallbackMethod = "chatStreamFallback")
    public Flux<String> chatStream(String sessionId, String userMessage) {
        // 히스토리 주입 -> 멀티턴 맥락 유지, 이전 대화 모르면 답변 일관성 깨짐
        List<Message> chatHistory = chatHistoryProvider.getRecentHistory(sessionId);
 
        log.debug("[LLM+RAG] 대화 히스토리 조회 완료 - sessionId: {}, 메시지 수: {}",
                sessionId, chatHistory.size());

        // 최근 사용자 메시지로 검색 -> 전체 히스토리보다 현재 질문에 집중해 검색 정확도 향상
        String lastUserMessage = chatHistory.stream()
                .filter(m -> m instanceof UserMessage)
                .reduce((first, second) -> second)
                .map(Message::getText)
                .orElse(userMessage);

        log.info("[LLM+RAG] VectorStore 검색 시작 - 쿼리 길이: {}, topK: {}, threshold: {}",
                lastUserMessage.length(), topK, similarityThreshold);

        // VectorStore에서 유사 문서 검색
        SearchRequest searchRequest = SearchRequest.builder()
                .query(lastUserMessage)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        List<Document> similarDocuments = vectorStore.similaritySearch(searchRequest);

        log.info("[LLM+RAG] 유사 문서 검색 완료 - 검색된 문서 수: {} (threshold: {})",
                similarDocuments.size(), similarityThreshold);

        if (similarDocuments.isEmpty()) {
            logDebugSearchInfo(lastUserMessage);
        }

        // RAG 컨텍스트를 포함한 메시지 리스트 구성
        List<Message> messagesWithRag = new ArrayList<>();

        if (!similarDocuments.isEmpty()) {
            String ragContext = similarDocuments.stream()
                    .map(doc -> {
                        log.debug("[LLM+RAG] 검색된 문서 - 유사도: {}, 내용 길이: {}",
                                doc.getMetadata().get("distance"),
                                doc.getText().length());
                        return doc.getText();
                    })
                    .collect(Collectors.joining("\n\n=== 참고 자료 구분 ===\n\n"));

            String systemPrompt = promptLoader.buildChatRagSystemPromptWithRule(ragContext);
            messagesWithRag.add(new SystemMessage(systemPrompt));
            log.debug("[LLM+RAG] 규칙 + RAG 컨텍스트 추가 - 전체 길이: {}", ragContext.length());
        } else {
            String systemRulePrompt = promptLoader.loadChatRulePrompt();
            messagesWithRag.add(new SystemMessage(systemRulePrompt));
            log.warn("[LLM+RAG] 유사 문서를 찾지 못했습니다. 규칙 프롬프트만 적용하여 진행합니다.");
        }

        messagesWithRag.addAll(chatHistory);

        ChatClient chatClient = chatClientBuilder.build();
        log.debug("[LLM+RAG] 최종 메시지 수: {} (RAG 포함)", messagesWithRag.size());

        // Flux 직접 반환(block 없음) -> 호출자가 논블로킹 구독, 토큰 단위로 SSE 실시간 푸시 가능
        return chatClient
                .prompt()
                .messages(messagesWithRag)
                .stream()
                .content()
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .doOnError(error ->
                        log.error("[LLM+RAG] 스트리밍 중 에러 발생 - sessionId: {}", sessionId, error))
                .doOnComplete(() ->
                        log.debug("[LLM+RAG] 스트리밍 완료 - sessionId: {}", sessionId));
    }

    // 유사 문서 검색 실패 시 디버깅용 재검색
    private void logDebugSearchInfo(String query) {
        SearchRequest debugRequest = SearchRequest.builder()
                .query(query)
                .topK(3)
                .similarityThreshold(0.0)
                .build();
        List<Document> debugDocs = vectorStore.similaritySearch(debugRequest);
        if (!debugDocs.isEmpty()) {
            log.warn("[LLM+RAG] 임계값({}) 때문에 문서가 필터링됨. 임계값 없이 검색 시 {}개 문서 발견.",
                    similarityThreshold, debugDocs.size());
            for (Document doc : debugDocs) {
                Object distanceObj = doc.getMetadata().get("distance");
                double distance = distanceObj != null ? ((Number) distanceObj).doubleValue() : 0.0;
                double similarity = 1.0 - distance;
                log.warn("[LLM+RAG] - 문서: {}, 거리: {}, 유사도: {} (threshold: {})",
                        doc.getMetadata().get("source"),
                        String.format("%.3f", distance),
                        String.format("%.3f", similarity),
                        similarityThreshold);
            }
        } else {
            log.warn("[LLM+RAG] VectorStore에 문서가 없습니다.");
        }
    }

    @SuppressWarnings("unused")
    private Flux<String> chatStreamFallback(String sessionId, String userMessage,
                                            CallNotPermittedException ex) {
        log.warn("[LLM+RAG] Circuit OPEN - 즉시 실패 반환 - sessionId: {}", sessionId);
        return Flux.error(new BusinessException(ErrorCode.LLM_CIRCUIT_OPEN));
    }

    @SuppressWarnings("unused")
    private Flux<String> chatStreamFallback(String sessionId, String userMessage,
                                            Throwable t) {
        log.error("[LLM+RAG] LLM 호출 실패(CB 카운트됨) - sessionId: {}, cause: {}",
                sessionId, t.getMessage());
        if (t instanceof BusinessException be) {
            return Flux.error(be);
        }
        return Flux.error(new BusinessException(ErrorCode.LLM_RESPONSE_FAIL));
    }

    @Override
    public String summaryChat(String sessionId) {
        try {
            // 윈도우 미적용 전량 조회 -> 이 요약은 PostgreSQL 에 영구 저장되므로 최근 N 턴만 보면 기록이 영구 손실
            List<Message> chatHistory = chatHistoryProvider.getFullHistory(sessionId);

            log.debug("[LLM+RAG] 대화 요약 시작 - sessionId: {}, 메시지 수: {}",
                    sessionId, chatHistory.size());

            // 프롬프트 로드 (서술적 요약 안내)
            String summaryPrompt = promptLoader.buildChatSummaryPrompt();

            // Spring AI ChatClient를 사용한 전체 응답 (RAG 없이 대화만 요약)
            ChatClient chatClient = chatClientBuilder.build();

            String summary = chatClient
                    .prompt()
                    .system(summaryPrompt)
                    .messages(chatHistory)
                    .call()
                    .content();

            log.info("[LLM+RAG] 대화 요약 완료 - sessionId: {}, 요약 길이: {}",
                    sessionId, summary != null ? summary.length() : 0);

            return summary;

        } catch (Exception e) {
            // 상세 예외 정보 로깅
            String errorType = e.getClass().getSimpleName();
            String errorMessage = e.getMessage();
            Throwable rootCause = e.getCause();
            String rootCauseMessage = rootCause != null ? rootCause.getMessage() : "없음";

            log.error("[LLM+RAG] 대화 요약 실패 - sessionId: {}, 예외타입: {}, 메시지: {}, 원인: {}",
                    sessionId, errorType, errorMessage, rootCauseMessage);
            log.error("[LLM+RAG] 상세 스택트레이스:", e);

            // API 키 관련 오류 감지
            if (errorMessage != null && (
                    errorMessage.contains("API key") ||
                            errorMessage.contains("api_key") ||
                            errorMessage.contains("Unauthorized") ||
                            errorMessage.contains("401") ||
                            errorMessage.contains("authentication"))) {
                log.error("[LLM+RAG] ⚠️ API 키 문제 의심 - OPENAI_API_KEY 환경변수를 확인하세요!");
            }

            throw new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);
        }
    }
}
