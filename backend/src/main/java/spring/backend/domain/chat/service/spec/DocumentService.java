package spring.backend.domain.chat.service.spec;

import org.springframework.ai.document.Document;

import java.util.List;

public interface DocumentService {
    void loadAndIndexDocuments();
    List<Document> loadPdfDocuments(String directoryPath);
    void indexDocuments(List<Document> documents);
    boolean isVectorStoreInitialized();
}
