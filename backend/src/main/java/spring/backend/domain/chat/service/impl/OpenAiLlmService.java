package spring.backend.domain.chat.service.impl;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
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
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

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
    private final ChatMemory chatMemory; // Spring AI ChatMemory 인터페이스 사용
    private final VectorStore vectorStore; // RAG용 VectorStore
    private final spring.backend.domain.chat.util.PromptLoader promptLoader;

    @Value("${app.rag.top-k:5}")
    private int topK;

    @Value("${app.rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Value("${spring.ai.openai.api-key:}")
    private String openaiApiKey;

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

    @Override
    public void chatStream(String sessionId, String userMessage, Consumer<String> chunkConsumer) {
        try {
            // Spring AI ChatMemory를 통해 대화 히스토리 가져오기
            List<Message> chatHistory = chatMemory.get(sessionId);

            log.debug("[LLM+RAG] 대화 히스토리 조회 완료 - sessionId: {}, 메시지 수: {}",
                    sessionId, chatHistory.size());

            // 가장 최근 사용자 메시지로 유사 문서 검색
            String lastUserMessage = chatHistory.stream()
                    .filter(m -> m instanceof UserMessage)
                    .reduce((first, second) -> second) // 마지막 메시지
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

            // 검색된 문서가 없으면 디버깅 정보 제공
            if (similarDocuments.isEmpty()) {
                SearchRequest debugRequest = SearchRequest.builder()
                        .query(lastUserMessage)
                        .topK(3)
                        .similarityThreshold(0.0)
                        .build();
                List<Document> debugDocs = vectorStore.similaritySearch(debugRequest);
                if (!debugDocs.isEmpty()) {
                    log.warn("[LLM+RAG] ⚠️ 임계값({}) 때문에 문서가 필터링됨. 임계값 없이 검색 시 {}개 문서 발견.",
                            similarityThreshold, debugDocs.size());
                    for (Document doc : debugDocs) {
                        Object distanceObj = doc.getMetadata().get("distance");
                        double distance = distanceObj != null ? ((Number) distanceObj).doubleValue() : 0.0;
                        double similarity = 1.0 - distance;  // Cosine Similarity = 1 - Cosine Distance
                        log.warn("[LLM+RAG] - 문서: {}, 거리: {}, 유사도: {} (threshold: {})",
                                doc.getMetadata().get("source"),
                                String.format("%.3f", distance),
                                String.format("%.3f", similarity),
                                similarityThreshold);
                    }
                } else {
                    log.warn("[LLM+RAG] ⚠️ VectorStore에 문서가 없습니다.");
                }
            }

            // RAG 컨텍스트를 포함한 메시지 리스트 구성
            List<Message> messagesWithRag = new ArrayList<>();

            // 유사 문서가 있으면 시스템 메시지로 추가
            if (!similarDocuments.isEmpty()) {
                String ragContext = similarDocuments.stream()
                        .map(doc -> {
                            log.debug("[LLM+RAG] 검색된 문서 - 유사도: {}, 내용 길이: {}",
                                    doc.getMetadata().get("distance"),
                                    doc.getText().length());
                            return doc.getText();
                        })
                        .collect(Collectors.joining("\n\n=== 참고 자료 구분 ===\n\n"));

                String systemPrompt = promptLoader.buildChatRagSystemPrompt(ragContext);

                messagesWithRag.add(new SystemMessage(systemPrompt));
                log.debug("[LLM+RAG] RAG 컨텍스트 추가 - 전체 길이: {}", ragContext.length());
            } else {
                log.warn("[LLM+RAG] 유사 문서를 찾지 못했습니다. 일반 대화로 진행합니다.");
            }

            // 기존 대화 히스토리 추가
            messagesWithRag.addAll(chatHistory);

            // Spring AI ChatClient를 사용한 스트리밍 (대화 히스토리 + RAG)
            ChatClient chatClient = chatClientBuilder.build();

            log.debug("[LLM+RAG] 최종 메시지 수: {} (RAG 포함)", messagesWithRag.size());

            Flux<String> streamResponse = chatClient
                    .prompt()
                    .messages(messagesWithRag)
                    .stream()
                    .content();

            // 각 청크를 Consumer에 전달
            streamResponse
                    .doOnNext(chunk -> {
                        if (chunk != null && !chunk.isEmpty()) {
                            chunkConsumer.accept(chunk);
                        }
                    })
                    .doOnError(error -> {
                        log.error("[LLM+RAG] 스트리밍 중 에러 발생 - sessionId: {}", sessionId, error);
                    })
                    .doOnComplete(() -> {
                        log.debug("[LLM+RAG] 스트리밍 완료 - sessionId: {}", sessionId);
                    })
                    .blockLast(); // 스트리밍 완료까지 대기

        } catch (Exception e) {
            // 상세 예외 정보 로깅
            String errorType = e.getClass().getSimpleName();
            String errorMessage = e.getMessage();
            Throwable rootCause = e.getCause();
            String rootCauseMessage = rootCause != null ? rootCause.getMessage() : "없음";

            log.error("[LLM+RAG] LLM 응답 실패 - sessionId: {}, 예외타입: {}, 메시지: {}, 원인: {}",
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

    @Override
    public String summaryChat(String sessionId) {
        try {
            // Spring AI ChatMemory를 통해 대화 히스토리 가져오기
            List<Message> chatHistory = chatMemory.get(sessionId);

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
