package spring.backend.domain.chat.service.impl;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import spring.backend.domain.chat.service.spec.RagService;
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
public class RagServiceImpl implements RagService {

    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;
    private final spring.backend.domain.chat.util.PromptLoader promptLoader;

    @Value("${app.rag.top-k:5}")
    private int topK;

    @Value("${app.rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Override
    @CircuitBreaker(name = "llm-rag", fallbackMethod = "generateSimilarProblemStreamFallback")
    public Flux<String> generateSimilarProblemStream(String problemContext) {
        log.info("[RAG] 오프너 분석 시작 - 문제 컨텍스트 길이: {}", problemContext.length());

        SearchRequest searchRequest = SearchRequest.builder()
                .query(problemContext)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        List<Document> similarDocuments = vectorStore.similaritySearch(searchRequest);

        log.info("[RAG] 유사 문서 검색 완료 - 검색된 문서 수: {} (threshold: {})",
                similarDocuments.size(), similarityThreshold);

        if (similarDocuments.isEmpty()) {
            logDebugSearchInfo(problemContext);
        }

        // 검색된 문서를 컨텍스트로 구성
        String retrievedContext = "";
        if (!similarDocuments.isEmpty()) {
            retrievedContext = similarDocuments.stream()
                    .map(doc -> {
                        log.debug("[RAG] 검색된 문서 - 유사도: {}, 내용 길이: {}",
                                doc.getMetadata().get("distance"),
                                doc.getText().length());
                        return doc.getText();
                    })
                    .collect(Collectors.joining("\n\n=== 참고 자료 구분 ===\n\n"));
        } else {
            log.warn("[RAG] 유사 문서를 찾지 못했습니다. 일반 LLM 응답으로 진행합니다.");
        }

        // 프롬프트 구성
        String prompt = !retrievedContext.isEmpty()
                ? promptLoader.buildRagOpenerAnalysisPrompt(retrievedContext, problemContext)
                : promptLoader.buildNoRagOpenerAnalysisPrompt(problemContext);

        ChatClient chatClient = chatClientBuilder.build();

        return chatClient
                .prompt()
                .user(prompt)
                .stream()
                .content()
                .filter(chunk -> chunk != null && !chunk.isEmpty())
                .doOnError(error -> log.error("[RAG] 스트리밍 중 에러 발생", error))
                .doOnComplete(() -> log.info("[RAG] 스트리밍 완료"));
    }

    private void logDebugSearchInfo(String query) {
        SearchRequest debugRequest = SearchRequest.builder()
                .query(query)
                .topK(3)
                .similarityThreshold(0.0)
                .build();
        List<Document> debugDocs = vectorStore.similaritySearch(debugRequest);
        if (!debugDocs.isEmpty()) {
            log.warn("[RAG] 임계값({}) 때문에 문서가 필터링됨. 임계값 없이 검색 시 {}개 문서 발견.",
                    similarityThreshold, debugDocs.size());
            for (Document doc : debugDocs) {
                Object distanceObj = doc.getMetadata().get("distance");
                double distance = distanceObj != null ? ((Number) distanceObj).doubleValue() : 0.0;
                double similarity = 1.0 - distance;
                log.warn("[RAG] - 문서: {}, 거리: {}, 유사도: {} (threshold: {})",
                        doc.getMetadata().get("source"),
                        String.format("%.3f", distance),
                        String.format("%.3f", similarity),
                        similarityThreshold);
            }
        } else {
            log.warn("[RAG] VectorStore에 문서가 없거나 임베딩 문제가 있습니다.");
        }
    }

    @SuppressWarnings("unused")
    private Flux<String> generateSimilarProblemStreamFallback(String problemContext,
                                                              CallNotPermittedException ex) {
        log.warn("[RAG] Circuit OPEN - 즉시 실패 반환");
        return Flux.error(new BusinessException(ErrorCode.LLM_CIRCUIT_OPEN));
    }

    @SuppressWarnings("unused")
    private Flux<String> generateSimilarProblemStreamFallback(String problemContext,
                                                              Throwable t) {
        log.error("[RAG] 오프너 분석 실패(CB 카운트됨) - cause: {}", t.getMessage());
        if (t instanceof BusinessException be) {
            return Flux.error(be);
        }
        return Flux.error(new BusinessException(ErrorCode.LLM_GENERATE_FAIL));
    }
}
