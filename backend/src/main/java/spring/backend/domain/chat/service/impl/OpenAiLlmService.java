package spring.backend.domain.chat.service.impl;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import spring.backend.domain.chat.service.spec.LlmService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@Service
@RequiredArgsConstructor
public class OpenAiLlmService implements LlmService {

    private final ChatClient.Builder chatClientBuilder;

    @Override
    public void chatStream(String sessionId, String userMessage, Consumer<String> chunkConsumer) {
        try {

            // Spring AI ChatClient를 사용한 스트리밍
            ChatClient chatClient = chatClientBuilder.build();

            Flux<String> streamResponse = chatClient
                    .prompt()
                    .user(userMessage)
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
                    })
                    .doOnComplete(() -> {
                    })
                    .blockLast(); // 스트리밍 완료까지 대기

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);
        }
    }
}
