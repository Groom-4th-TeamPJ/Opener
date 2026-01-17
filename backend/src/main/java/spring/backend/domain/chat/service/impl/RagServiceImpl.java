package spring.backend.domain.chat.service.impl;

import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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

    @Value("${app.rag.top-k:5}")
    private int topK;

    @Value("${app.rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Override
    public void generateSimilarProblemStream(String problemContext, Consumer<String> chunkConsumer) {
        try {
            // 문제 풀이 분석용 프롬프트 구성
            String prompt = String.format("""
                    다음은 학생이 틀린 수학 문제입니다:
                    
                    %s
                    
                    이 문제를 자세하게 분석하고 풀이 방법을 설명해주세요.
                    
                    다음 내용을 포함해주세요:
                    1. 문제가 요구하는 핵심 개념
                    2. 단계별 풀이 과정 (각 단계의 이유를 설명)
                    3. 정답이 왜 정답인지에 대한 설명
                    4. 학생들이 자주 하는 실수나 주의할 점
                    5. 이 문제를 푸는 데 필요한 기본 지식이나 공식
                    
                    학생이 이해하기 쉽도록 친절하고 자세하게 설명해주세요.
                    """, problemContext);

            // ChatClient 생성
            ChatClient chatClient = chatClientBuilder.build();

            // 스트리밍 호출
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
                    })
                    .blockLast();

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);
        }
    }
}
