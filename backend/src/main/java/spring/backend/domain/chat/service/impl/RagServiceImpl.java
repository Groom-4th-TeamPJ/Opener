package spring.backend.domain.chat.service.impl;

import java.util.List;
import java.util.function.Consumer;
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
    public void generateSimilarProblemStream(String problemContext, Consumer<String> chunkConsumer) {
        try {
            log.info("[RAG] 오프너 분석 시작 - 문제 컨텍스트 길이: {}", problemContext.length());

            // 1. VectorStore에서 유사 문서 검색
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(problemContext)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();

            List<Document> similarDocuments = vectorStore.similaritySearch(searchRequest);

            log.info("[RAG] 유사 문서 검색 완료 - 검색된 문서 수: {}", similarDocuments.size());

            // 2. 검색된 문서를 컨텍스트로 구성
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

                log.debug("[RAG] 전체 검색 컨텍스트 길이: {}", retrievedContext.length());
            } else {
                log.warn("[RAG] 유사 문서를 찾지 못했습니다. 일반 LLM 응답으로 진행합니다.");
            }

            // 3. 검색된 문서 + 문제를 프롬프트에 포함
            String prompt;
            if (!retrievedContext.isEmpty()) {
                // RAG 사용: 검색된 문서 포함
                prompt = promptLoader.buildRagOpenerAnalysisPrompt(retrievedContext, problemContext);
            } else {
                // RAG 실패 시 일반 프롬프트
                prompt = promptLoader.buildNoRagOpenerAnalysisPrompt(problemContext);
            }

            log.debug("[RAG] 프롬프트 구성 완료 - 전체 길이: {}", prompt.length());

            // 4. ChatClient를 사용한 스트리밍 호출
            ChatClient chatClient = chatClientBuilder.build();

            Flux<String> streamResponse = chatClient
                    .prompt()
                    .user(prompt)
                    .stream()
                    .content();

            // 청크 전달
            streamResponse
                    .doOnNext(chunk -> {
                        if (chunk != null && !chunk.isEmpty()) {
                            chunkConsumer.accept(chunk);
                        }
                    })
                    .doOnError(error -> {
                        log.error("[RAG] 스트리밍 중 에러 발생", error);
                    })
                    .doOnComplete(() -> {
                        log.info("[RAG] 스트리밍 완료");
                    })
                    .blockLast();

        } catch (Exception e) {
            log.error("[RAG] RAG 서비스 실행 실패", e);
            throw new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);
        }
    }
}
