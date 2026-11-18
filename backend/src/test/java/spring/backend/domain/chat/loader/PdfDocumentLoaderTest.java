package spring.backend.domain.chat.loader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import spring.backend.domain.chat.service.spec.DocumentService;

// 429 대기로 색인이 몇 시간 걸려도 기동과 readiness 를 붙잡지 않는다 -> run 은 색인을 백그라운드 스레드에 맡기고 바로 돌아온다
class PdfDocumentLoaderTest {

    @Test
    @DisplayName("색인이 끝나지 않아도 run 은 바로 돌아오고 색인은 다른 스레드에서 돈다")
    void run_색인은백그라운드_기동은안막힘() throws Exception {
        DocumentService documentService = mock(DocumentService.class);
        CountDownLatch release = new CountDownLatch(1);
        Thread[] indexingThread = new Thread[1];
        doAnswer(inv -> {
            indexingThread[0] = Thread.currentThread();
            release.await(10, TimeUnit.SECONDS);
            return null;
        }).when(documentService).loadAndIndexDocuments();

        PdfDocumentLoader loader = new PdfDocumentLoader(documentService);
        try {
            assertTimeoutPreemptively(Duration.ofSeconds(2), () -> loader.run(null));
            verify(documentService, timeout(2_000)).loadAndIndexDocuments();
            org.junit.jupiter.api.Assertions.assertNotSame(Thread.currentThread(), indexingThread[0]);
            org.junit.jupiter.api.Assertions.assertTrue(indexingThread[0].isVirtual());
        } finally {
            release.countDown();
        }
    }

    @Test
    @DisplayName("색인이 예외로 끝나도 run 은 던지지 않는다")
    void run_색인예외_기동은계속() {
        DocumentService documentService = mock(DocumentService.class);
        doAnswer(inv -> {
            throw new IllegalStateException("Voyage 429 포기");
        }).when(documentService).loadAndIndexDocuments();

        assertDoesNotThrow(() -> new PdfDocumentLoader(documentService).run(null));
        verify(documentService, timeout(2_000)).loadAndIndexDocuments();
    }
}
