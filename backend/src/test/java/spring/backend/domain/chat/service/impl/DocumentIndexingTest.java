package spring.backend.domain.chat.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

// 색인 결정은 "저장소 행 수 vs 이번에 만든 조각 수" 로 한다 -> 행이 절반만 찼을 때 영원히 건너뛰는 것이 429 포기 뒤의 위험이다
class DocumentIndexingTest {

    private static final int RECORDS = 1_200;

    @TempDir
    Path dir;

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final TokenTextSplitter splitter = new TokenTextSplitter();
    private final DocumentServiceImpl service = new DocumentServiceImpl(vectorStore, splitter, jdbc);
    private int expected;

    @BeforeEach
    void writeCorpus() throws Exception {
        StringBuilder jsonl = new StringBuilder();
        for (int i = 0; i < RECORDS; i++) {
            jsonl.append("{\"problem\":\"문제 ").append(i).append("\",\"solution\":\"풀이 ").append(i)
                    .append("\",\"level\":\"Level 1\",\"subject\":\"Algebra\"}\n");
        }
        Files.writeString(dir.resolve("a.jsonl"), jsonl.toString(), StandardCharsets.UTF_8);
        ReflectionTestUtils.setField(service, "documentsDirectory", dir.toUri().toString());
        ReflectionTestUtils.setField(service, "excludeFiles", List.of());
        expected = splitter.apply(service.loadJsonlDocuments(dir.toUri().toString())).size();
        assertTrue(expected > 1_000, "시험 자료가 슬라이스 3개를 만들 만큼 크지 않음: " + expected);
    }

    private void rowsInStore(long rows) {
        when(jdbc.queryForObject(anyString(), eq(Long.class))).thenReturn(rows);
    }

    @Test
    @DisplayName("행이 0개면 조각 전부를 500개 이하 슬라이스로 나눠 add 한다")
    void load_빈저장소_500개슬라이스로색인() {
        rowsInStore(0);

        service.loadAndIndexDocuments();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> slices = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, org.mockito.Mockito.atLeast(3)).add(slices.capture());
        List<Integer> sizes = slices.getAllValues().stream().map(List::size).toList();
        assertTrue(sizes.stream().allMatch(n -> n <= 500), "슬라이스 크기: " + sizes);
        assertEquals(expected, sizes.stream().mapToInt(Integer::intValue).sum());
        verify(jdbc, never()).update(anyString());
        assertTrue(service.isVectorStoreInitialized());
    }

    @Test
    @DisplayName("행 수가 이번에 만든 조각 수와 같으면 임베딩 호출 없이 건너뛴다")
    void load_행수일치_건너뜀() {
        rowsInStore(expected);

        service.loadAndIndexDocuments();

        verify(vectorStore, never()).add(anyList());
        verify(vectorStore, never()).similaritySearch(org.mockito.ArgumentMatchers.any(org.springframework.ai.vectorstore.SearchRequest.class));
        verify(jdbc, never()).update(anyString());
        assertTrue(service.isVectorStoreInitialized());
    }

    @Test
    @DisplayName("행이 0 보다 많고 조각 수보다 적으면 전부 지우고 다시 색인한다")
    void load_일부만찬저장소_전부지우고재색인() {
        rowsInStore(expected - 10);

        service.loadAndIndexDocuments();

        InOrder order = inOrder(jdbc, vectorStore);
        order.verify(jdbc).update("DELETE FROM vector_store");
        order.verify(vectorStore, org.mockito.Mockito.atLeast(3)).add(anyList());
        assertTrue(service.isVectorStoreInitialized());
    }

    @Test
    @DisplayName("행이 조각 수보다 많으면 지우지도 다시 넣지도 않고 건너뛴다")
    void load_행이더많음_경고후건너뜀() {
        rowsInStore(expected + 5);

        service.loadAndIndexDocuments();

        verify(vectorStore, never()).add(anyList());
        verify(jdbc, never()).update(anyString());
        assertTrue(service.isVectorStoreInitialized());
    }

    // 429 로 포기하면 add 가 던진다 -> 끝낸 슬라이스는 이미 저장돼 있고 초기화 표시는 서지 않는다
    @Test
    @DisplayName("두 번째 슬라이스에서 add 가 실패하면 첫 슬라이스는 저장된 채 예외가 올라가고 초기화 표시는 서지 않는다")
    void load_중간슬라이스실패_앞슬라이스유지_예외전파() {
        rowsInStore(0);
        doNothing().doThrow(new IllegalStateException("Voyage 429 포기")).when(vectorStore).add(anyList());

        assertThrows(IllegalStateException.class, () -> service.loadAndIndexDocuments());

        verify(vectorStore, org.mockito.Mockito.times(2)).add(anyList());
        assertEquals(false, service.isVectorStoreInitialized());
    }

    @Test
    @DisplayName("문서가 하나도 없으면 행 수를 세지도 add 하지도 않는다")
    void load_문서없음_아무것도안함() throws Exception {
        Files.delete(dir.resolve("a.jsonl"));

        service.loadAndIndexDocuments();

        verify(vectorStore, never()).add(anyList());
        verify(jdbc, never()).update(anyString());
    }
}
