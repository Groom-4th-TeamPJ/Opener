package spring.backend.domain.chat.service.impl;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
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

// @ConditionalOnProperty -> RAG 비활성 환경에선 이 빈 자체를 안 만들어 VectorStore 미존재 시 부팅 실패 방지
// 단 빈을 안 만드는 것만으로는 방어가 안 된다 -> ChatServiceImpl 이 RagService 를 필수 주입받으므로
// havingValue="false" 짝인 DisabledRagService 가 함께 있어야 비로소 성립한다
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
    private final MeterRegistry meterRegistry;

    // @Value 외부화 -> 검색 개수/임계값을 코드 수정·재배포 없이 yml 로 튜닝
    @Value("${app.rag.top-k:5}")
    private int topK;

    // 임계값 -> 무관한 문서가 프롬프트에 섞여 답변 품질을 떨어뜨리는 것 차단
    @Value("${app.rag.similarity-threshold:0.2}")
    private double similarityThreshold;

    // @CircuitBreaker -> OpenAI 장애·지연이 길어지면 즉시 실패시켜, SSE 스레드가 응답 대기에 묶이는 연쇄 지연 차단
    @Override
    @CircuitBreaker(name = "llm-rag", fallbackMethod = "generateSimilarProblemStreamFallback")
    public Flux<String> generateSimilarProblemStream(String problemContext) {
        log.info("[RAG] 오프너 분석 시작 - 문제 컨텍스트 길이: {}", problemContext.length());

        SearchRequest searchRequest = SearchRequest.builder()
                .query(problemContext)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();

        // 벡터 검색 소요를 Timer 로 계측 (rag.vector.search) -> 대시보드 p95 패널
        // path 태그는 일반 채팅 경로와 짝 -> 한쪽만 태그를 붙이면 같은 미터에 태그 집합이 갈려 예외
        List<Document> similarDocuments = meterRegistry.timer("rag.vector.search", "path", "opener")
                .record(() -> vectorStore.similaritySearch(searchRequest));

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

        // 검색 결과 유무로 프롬프트 분기 -> 자료 없을 때 빈 컨텍스트로 환각 유도하지 않고 일반 분석으로 폴백
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

    // fallback 2개로 오버로드 -> Circuit OPEN(차단)과 일반 호출 실패를 다른 에러코드로 구분 응답
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
