package spring.backend.domain.chat.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import spring.backend.domain.chat.service.spec.DocumentService;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.rag",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class DocumentServiceImpl implements DocumentService {

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Value("${app.documents.directory:classpath:documents/}")
    private String documentsDirectory;

    @Override
    public void loadAndIndexDocuments() {
        if (initialized.get()) {
            log.info("Vector store already initialized");
            return;
        }

        List<Document> documents = loadPdfDocuments(documentsDirectory);

        if (documents.isEmpty()) {
            log.warn("No PDF documents found in {}", documentsDirectory);
            return;
        }

        indexDocuments(documents);
        initialized.set(true);
        log.info("Vector store initialized successfully with {} documents", documents.size());
    }

    @Override
    public List<Document> loadPdfDocuments(String directoryPath) {
        List<Document> allDocuments = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        try {
            // documents에 적재한 pdf 문서 가져오기
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(directoryPath + "**/*.pdf");

            log.info("Found {} PDF files in {}", resources.length, directoryPath);

            for (Resource resource : resources) {
                try {
                    log.info("Loading PDF: {}", resource.getFilename());

                    PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(resource);
                    List<Document> docs = pdfReader.get();

                    // 메타데이터 추가
                    docs.forEach(doc -> {
                        doc.getMetadata().put("source", resource.getFilename());
                        doc.getMetadata().put("loaded_at", LocalDateTime.now().toString());
                    });

                    allDocuments.addAll(docs);
                    successCount++;
                    log.info("✓ Successfully loaded {} pages from {}", docs.size(), resource.getFilename());

                } catch (Exception e) {
                    failCount++;
                    log.warn("✗ Failed to load PDF: {} - Error: {}", resource.getFilename(), e.getMessage());
                    log.debug("Detailed error for {}", resource.getFilename(), e);
                }
            }

            log.info("PDF loading completed - Success: {}, Failed: {}, Total pages loaded: {}",
                    successCount, failCount, allDocuments.size());

        } catch (Exception e) {
            log.error("Failed to scan PDF directory: {}", directoryPath, e);
        }

        return allDocuments;
    }

    @Override
    public void indexDocuments(List<Document> documents) {
        // 청킹
        List<Document> chunks = textSplitter.apply(documents);

        log.info("Split into {} chunks, indexing into vector store...", chunks.size());

        // 벡터 스토어에 저장 (자동으로 임베딩 생성)
        vectorStore.add(chunks);

        log.info("Indexing completed");
    }

    @Override
    public boolean isVectorStoreInitialized() {
        return initialized.get();
    }
}
