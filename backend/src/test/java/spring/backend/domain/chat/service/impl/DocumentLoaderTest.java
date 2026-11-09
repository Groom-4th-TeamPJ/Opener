package spring.backend.domain.chat.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;
import org.yaml.snakeyaml.Yaml;

// ko_math-500.jsonl 은 problem/solution/subject 키를 써서 problem_ko/solution_ko/type 만 읽는 로더가 500행을 빈 껍데기로 색인했다
class DocumentLoaderTest {

    private static final String SHAPE_KO = "{\"problem_ko\":\"문제 가\",\"solution_ko\":\"풀이 가\",\"level\":\"Level 1\",\"type\":\"Algebra\"}";
    private static final String SHAPE_PLAIN = "{\"problem\":\"문제 나\",\"solution\":\"풀이 나\",\"level\":\"Level 2\",\"subject\":\"Geometry\"}";
    private static final String BLANK = "{\"problem\":\"\",\"solution\":\"\",\"subject\":\"Algebra\"}";

    @TempDir
    Path dir;

    private final DocumentServiceImpl service =
            new DocumentServiceImpl(mock(VectorStore.class), new TokenTextSplitter());

    @Test
    @DisplayName("problem_ko 키와 problem 키 레코드가 둘 다 본문이 있는 문서가 된다")
    void loadJsonlDocuments_두키형태_본문있는문서() throws Exception {
        Files.writeString(dir.resolve("a.jsonl"), SHAPE_KO + "\n" + SHAPE_PLAIN + "\n", StandardCharsets.UTF_8);

        List<Document> docs = load();

        assertEquals(2, docs.size());
        Document ko = byText(docs, "문제 가");
        Document plain = byText(docs, "문제 나");
        assertTrue(ko.getText().contains("풀이 가"));
        assertTrue(plain.getText().contains("풀이 나"));
        assertEquals("Algebra", ko.getMetadata().get("type"));
        // type 키가 없으면 subject 가 분류를 대신한다
        assertEquals("Geometry", plain.getMetadata().get("type"));
    }

    @Test
    @DisplayName("문제와 풀이가 모두 빈 레코드는 문서로 만들지 않는다")
    void loadJsonlDocuments_빈레코드_건너뜀() throws Exception {
        Files.writeString(dir.resolve("a.jsonl"), BLANK + "\n" + SHAPE_PLAIN + "\n", StandardCharsets.UTF_8);

        List<Document> docs = load();

        assertEquals(1, docs.size());
        assertFalse(docs.get(0).getText().isBlank());
    }

    @Test
    @DisplayName("제외 목록에 ko_math-500.jsonl 이 있고 로더가 그 파일을 읽지 않는다")
    void loadJsonlDocuments_제외목록_파일건너뜀() throws Exception {
        List<String> excluded = loadExcludeFilesFromYaml();
        assertTrue(excluded.contains("ko_math-500.jsonl"), "application.yml 제외 목록: " + excluded);

        Files.writeString(dir.resolve("ko_math-500.jsonl"), SHAPE_PLAIN + "\n", StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("keep.jsonl"), SHAPE_KO + "\n", StandardCharsets.UTF_8);
        ReflectionTestUtils.setField(service, "excludeFiles", excluded);

        List<Document> docs = load();

        assertEquals(1, docs.size());
        assertEquals("keep.jsonl", docs.get(0).getMetadata().get("source"));
    }

    private List<Document> load() {
        return service.loadJsonlDocuments(dir.toUri().toString());
    }

    private static Document byText(List<Document> docs, String problem) {
        return docs.stream().filter(d -> d.getText().contains(problem)).findFirst().orElseThrow();
    }

    // @Value 의 List 변환은 쉼표 구분 문자열을 받는다 -> yml 값도 쉼표 문자열로 두고 같은 방식으로 쪼갠다
    @SuppressWarnings("unchecked")
    private static List<String> loadExcludeFilesFromYaml() throws Exception {
        try (InputStream in = DocumentLoaderTest.class.getClassLoader().getResourceAsStream("application.yml")) {
            assertNotNull(in, "classpath 에 application.yml 없음");
            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> app = (Map<String, Object>) root.get("app");
            Map<String, Object> rag = (Map<String, Object>) app.get("rag");
            Map<String, Object> index = (Map<String, Object>) rag.get("index");
            String raw = String.valueOf(index.get("exclude-files"));
            return Arrays.stream(raw.split(",")).map(String::trim).collect(Collectors.toList());
        }
    }
}
