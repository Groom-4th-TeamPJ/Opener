package spring.backend.domain.chat.service.impl;

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

            log.debug("[LLM+RAG] VectorStore 검색 시작 - 쿼리 길이: {}", lastUserMessage.length());

            // VectorStore에서 유사 문서 검색
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(lastUserMessage)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();

            List<Document> similarDocuments = vectorStore.similaritySearch(searchRequest);

            log.info("[LLM+RAG] 유사 문서 검색 완료 - 검색된 문서 수: {}", similarDocuments.size());

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
            log.error("[LLM+RAG] LLM 응답 실패 - sessionId: {}", sessionId, e);
            throw new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);
        }
    }
}
