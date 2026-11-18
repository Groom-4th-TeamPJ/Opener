package spring.backend.domain.chat.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import spring.backend.domain.chat.service.spec.DocumentService;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.rag",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class PdfDocumentLoader implements ApplicationRunner {

    private final DocumentService documentService;

    // 색인은 가상 스레드에서 돈다 -> Voyage 429 대기로 몇 시간 걸려도 기동과 readiness 를 붙잡지 않는다
    // 색인 중의 검색은 이미 저장된 슬라이스만 보고, 아직 없으면 문서 0건으로 규칙 프롬프트만 쓴다 (RagLlmService 의 빈 결과 경로)
    @Override
    public void run(ApplicationArguments args) {
        Thread.ofVirtual().name("rag-index").start(() -> {
            long startedAt = System.nanoTime();
            log.info("[RAG-INDEX] 색인 시작");
            try {
                documentService.loadAndIndexDocuments();
                log.info("[RAG-INDEX] 색인 종료 - {} s", (System.nanoTime() - startedAt) / 1_000_000_000L);
            } catch (Exception e) {
                // 서버는 계속 돌고 일반 채팅은 문서 없이 가능 -> 다음 기동에서 행 수를 다시 비교해 이어서 처리한다
                log.error("[RAG-INDEX] 색인 실패 - {} s, 다음 기동 때 다시 시도", (System.nanoTime() - startedAt) / 1_000_000_000L, e);
            }
        });
    }
}
