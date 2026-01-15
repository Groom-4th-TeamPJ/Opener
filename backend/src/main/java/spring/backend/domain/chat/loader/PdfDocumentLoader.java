package spring.backend.domain.chat.loader;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import spring.backend.domain.chat.service.spec.DocumentService;

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

    @Override
    public void run(ApplicationArguments args) {
        try {
            documentService.loadAndIndexDocuments();
        } catch (Exception e) {
            // 서버 시작은 계속 진행 (문서 없이도 일반 채팅 가능)
        }
    }
}
