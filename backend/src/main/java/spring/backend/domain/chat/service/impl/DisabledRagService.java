package spring.backend.domain.chat.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import spring.backend.domain.chat.service.spec.RagService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

/**
 * RAG 비활성 환경용 RagService 구현체
 *
 * <p>RagServiceImpl 이 havingValue="true" 로 막혀 있어 플래그를 끄면 RagService 빈이 사라진다.
 * ChatServiceImpl 은 이를 필수 생성자 인자로 받으므로 짝이 되는 구현체가 없으면
 * ApplicationContext 기동 자체가 실패한다 — 오프너 분석만 못 쓰는 것이 아니라 채팅 도메인 전체가 죽는다.
 * LlmService 가 OpenAiLlmService / OpenAiLlmServiceWithoutRag 짝으로 어떤 설정에서도
 * 빈이 하나는 생기는 것과 같은 구조를 RagService 에도 맞춘 것이다.
 *
 * <p>VectorStore·ChatClient 를 주입받지 않으므로 벡터 저장소가 없는 환경에서도 뜬다.
 */
@Slf4j
@Service
@ConditionalOnProperty(
        prefix = "app.rag",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true // RAG 설정이 없으면 이 구현체 사용
)
public class DisabledRagService implements RagService {

    // 빈 스트림이 아니라 error -> 조용히 성공한 것처럼 끝나면 호출부가 빈 응답을 정상 완료로 처리한다
    // (STREAM_START 없이 doOnComplete 가 도는 그 경로가 세션 고착의 원인이었다)
    @Override
    public Flux<String> generateSimilarProblemStream(String problemContext) {
        log.warn("[RAG] 비활성 상태에서 오프너 분석 요청 - app.rag.enabled 를 켜야 동작한다");
        return Flux.error(new BusinessException(ErrorCode.RAG_DISABLED));
    }
}
