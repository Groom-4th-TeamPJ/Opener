package spring.backend.domain.chat.service.impl;

import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

/**
 * RAG가 비활성화되었을 때 사용하는 LLM 서비스
 * 멀티턴 대화는 지원하지만 RAG는 사용하지 않음
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
    private final ChatMemory chatMemory; // Spring AI ChatMemory 인터페이스 사용

    @Override
    public void chatStream(String sessionId, String userMessage, Consumer<String> chunkConsumer) {
        try {
            // Spring AI ChatMemory를 통해 대화 히스토리 가져오기
            List<Message> chatHistory = chatMemory.get(sessionId);

            log.debug("[LLM-NoRAG] 대화 히스토리 조회 완료 - sessionId: {}, 메시지 수: {}",
                    sessionId, chatHistory.size());

            // 새로운 사용자 메시지는 이미 Redis에 저장되어 chatHistory에 포함되어 있으므로
            // 별도로 추가하지 않습니다 (ChatServiceImpl에서 저장 후 호출)

            // Spring AI ChatClient를 사용한 스트리밍 (대화 히스토리만, RAG 없음)
            ChatClient chatClient = chatClientBuilder.build();

            log.debug("[LLM-NoRAG] RAG 비활성화 상태 - 멀티턴 대화만 지원");

            Flux<String> streamResponse = chatClient
                    .prompt()
                    .messages(chatHistory)
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
                        log.error("[LLM-NoRAG] 스트리밍 중 에러 발생 - sessionId: {}", sessionId, error);
                    })
                    .doOnComplete(() -> {
                        log.debug("[LLM-NoRAG] 스트리밍 완료 - sessionId: {}", sessionId);
                    })
                    .blockLast(); // 스트리밍 완료까지 대기

        } catch (Exception e) {
            log.error("[LLM-NoRAG] LLM 응답 실패 - sessionId: {}", sessionId, e);
            throw new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);
        }
    }
}
