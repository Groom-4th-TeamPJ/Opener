package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
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

    // [asy] 는 문제의 그림을 그리는 Asymptote 소스 코드 -> 색인 대상에서 걷어낸다
    // 블록을 가진 문서에서 글자수의 평균 48% 를 차지한다
    // 가설: 임베딩이 좌표·draw 명령 쪽으로 기울어 자연어 한 문장인 질문과 맞물리지 않는다
    // 임계값 0.7 에서 20문항 전부 검색 0건이었지만 원인이 [asy] 인지는 검증되지 않았다
    // 임계값 스윕에서는 [asy] 문서가 오히려 더 자주 검색됐다(21% 대 13.6%)
    private static final Pattern ASY_BLOCK = Pattern.compile("\\[asy].*?\\[/asy]", Pattern.DOTALL);

    // 블록을 지우고 남은 빈 줄이 3줄 이상이면 2줄로 접는다
    private static final Pattern BLANK_RUN = Pattern.compile("\\n{3,}");

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Value("${app.documents.directory:classpath:documents/}")
    private String documentsDirectory;

    // 색인에서 뺄 JSONL 파일명 -> 기본값은 MATH-ko-test 와 수식 기준 176행 이상 겹치는 ko_math-500.jsonl
    @Value("${app.rag.index.exclude-files:ko_math-500.jsonl}")
    private List<String> excludeFiles = List.of();

    @Override
    public void loadAndIndexDocuments() {
        if (initialized.get()) {
            log.info("Vector store already initialized (in-memory flag)");
            return;
        }

        // DB에 기존 문서가 있는지 확인 (간단한 검색으로 체크)
        try {
            SearchRequest checkRequest = SearchRequest.builder()
                    .query("math")  // 더미 쿼리
                    .topK(1)
                    .similarityThreshold(0.0)  // 임계값 0으로 모든 문서 검색
                    .build();
            List<Document> existingDocs = vectorStore.similaritySearch(checkRequest);

            if (!existingDocs.isEmpty()) {
                log.info("Vector store already contains {} document(s). Skipping re-indexing.", existingDocs.size());
                initialized.set(true);
                return;
            }
        } catch (Exception e) {
            log.warn("Failed to check existing documents: {}. Proceeding with indexing.", e.getMessage());
        }

        // PDF 문서 로드
        List<Document> pdfDocuments = loadPdfDocuments(documentsDirectory);
        log.info("Loaded {} PDF documents", pdfDocuments.size());

        // JSONL 문서 로드
        List<Document> jsonlDocuments = loadJsonlDocuments(documentsDirectory);
        log.info("Loaded {} JSONL documents", jsonlDocuments.size());

        // 모든 문서 병합
        List<Document> allDocuments = new ArrayList<>();
        allDocuments.addAll(pdfDocuments);
        allDocuments.addAll(jsonlDocuments);

        if (allDocuments.isEmpty()) {
            log.warn("No documents found in {}", documentsDirectory);
            return;
        }

        indexDocuments(allDocuments);
        initialized.set(true);
        log.info("Vector store initialized successfully with {} documents (PDF: {}, JSONL: {})",
                allDocuments.size(), pdfDocuments.size(), jsonlDocuments.size());
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

    /**
     * JSONL 파일에서 문서를 로드합니다.
     * 각 줄은 하나의 JSON 객체이며, 문제·풀이를 결합하여 문서로 변환합니다.
     * 키는 problem_ko/solution_ko 와 problem/solution 두 형태를, 분류는 type 과 subject 를 받습니다.
     */
    public List<Document> loadJsonlDocuments(String directoryPath) {
        List<Document> allDocuments = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        int successCount = 0;
        int failCount = 0;
        int asyStrippedCount = 0;
        int blankSkippedCount = 0;

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(directoryPath + "**/*.jsonl");

            log.info("Found {} JSONL files in {}", resources.length, directoryPath);

            for (Resource resource : resources) {
                if (excludeFiles.contains(resource.getFilename())) {
                    log.info("Skipping excluded JSONL: {}", resource.getFilename());
                    continue;
                }
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

                    log.info("Loading JSONL: {}", resource.getFilename());
                    String line;
                    int lineNumber = 0;

                    while ((line = reader.readLine()) != null) {
                        lineNumber++;
                        if (line.trim().isEmpty()) continue;

                        try {
                            JsonNode node = objectMapper.readTree(line);

                            // 문제와 풀이를 결합하여 문서 내용 생성
                            String rawProblem = firstText(node, "problem_ko", "problem");
                            String rawSolution = firstText(node, "solution_ko", "solution");
                            String level = firstText(node, "level");
                            // subject 는 type 이 없는 파일의 분류 키
                            String type = firstText(node, "type", "subject");

                            String problem = stripAsyBlocks(rawProblem);
                            String solution = stripAsyBlocks(rawSolution);
                            // 본문이 없는 레코드는 제목 틀만 있는 빈 청크가 되어 검색 결과 자리만 차지한다
                            if (problem.isBlank() && solution.isBlank()) {
                                blankSkippedCount++;
                                continue;
                            }
                            if (!problem.equals(rawProblem) || !solution.equals(rawSolution)) {
                                asyStrippedCount++;
                            }

                            // 문서 내용: 문제 + 풀이
                            String content = String.format(
                                    "[%s] %s\n\n## 문제\n%s\n\n## 풀이\n%s",
                                    type, level, problem, solution
                            );

                            // 메타데이터 구성
                            Map<String, Object> metadata = new HashMap<>();
                            metadata.put("source", resource.getFilename());
                            metadata.put("line_number", lineNumber);
                            metadata.put("type", type);
                            metadata.put("level", level);
                            metadata.put("loaded_at", LocalDateTime.now().toString());

                            Document doc = new Document(content, metadata);
                            allDocuments.add(doc);
                            successCount++;

                        } catch (Exception e) {
                            failCount++;
                            log.warn("Failed to parse line {} in {}: {}", lineNumber, resource.getFilename(), e.getMessage());
                        }
                    }

                    log.info("✓ Successfully loaded {} problems from {}", successCount, resource.getFilename());

                } catch (Exception e) {
                    log.error("Failed to read JSONL file: {} - Error: {}", resource.getFilename(), e.getMessage());
                }
            }

            log.info("JSONL loading completed - Success: {}, Failed: {}, Blank skipped: {}, Asymptote blocks stripped from: {} problems",
                    successCount, failCount, blankSkippedCount, asyStrippedCount);

        } catch (Exception e) {
            log.error("Failed to scan JSONL directory: {}", directoryPath, e);
        }

        return allDocuments;
    }

    // 앞쪽 키부터 보고 값이 비어 있으면 다음 키로 넘어간다 -> null 값은 "null" 문자열이 아니라 빈 문자열로 취급
    private static String firstText(JsonNode node, String... keys) {
        for (String key : keys) {
            String value = node.path(key).asText("");
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    // 닫히지 않은 [asy] 는 남긴다 -> 본문 끝까지 지워 문제 자체가 사라지는 것보다 낫다
    static String stripAsyBlocks(String text) {
        if (text == null || !text.contains("[asy]")) {
            return text == null ? "" : text;
        }
        String stripped = ASY_BLOCK.matcher(text).replaceAll("");
        return BLANK_RUN.matcher(stripped).replaceAll("\n\n").trim();
    }

    @Override
    public void indexDocuments(List<Document> documents) {
        log.info("Starting document indexing - Input documents: {}", documents.size());

        // 청킹 전 문서 정보 로깅
        for (Document doc : documents) {
            log.debug("Before chunking - Source: {}, Content length: {} chars",
                    doc.getMetadata().get("source"),
                    doc.getText().length());
        }

        // 청킹
        List<Document> chunks = textSplitter.apply(documents);

        log.info("Chunking completed - Input: {} documents → Output: {} chunks", documents.size(), chunks.size());

        // 청킹 결과 상세 로깅
        if (chunks.size() == documents.size()) {
            log.warn("⚠️ Chunking may not have worked properly! " +
                    "Input and output document count are the same ({} each). " +
                    "Check if PDF content is too short or has special characters.",
                    documents.size());
        }

        // 벡터 스토어에 저장 (자동으로 임베딩 생성)
        vectorStore.add(chunks);

        log.info("✅ Indexing completed - {} chunks stored in vector store", chunks.size());
    }

    @Override
    public boolean isVectorStoreInitialized() {
        return initialized.get();
    }
}
