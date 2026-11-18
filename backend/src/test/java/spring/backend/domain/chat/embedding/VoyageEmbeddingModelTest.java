package spring.backend.domain.chat.embedding;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;

// 로컬 HTTP 스텁만 쓴다 -> Voyage 실호출 없이 요청 모양, 분할, 순서 복원, 429 재시도, 차원 검사를 확인
class VoyageEmbeddingModelTest {

    private static final int DIM = 4;
    private static final ObjectMapper JSON = new ObjectMapper();

    private HttpServer server;
    private final List<Recorded> requests = new CopyOnWriteArrayList<>();
    private final List<Long> sleeps = new ArrayList<>();
    // 요청 본문을 받아 응답을 정하는 스텁 -> 기본은 입력 문자열 "t<n>" 의 n 으로 채운 벡터를 역순으로 돌려준다
    private volatile Function<Recorded, Reply> handler = VoyageEmbeddingModelTest::echoReversed;

    record Recorded(String path, String auth, JsonNode body) {}

    record Reply(int status, Map<String, String> headers, String body) {}

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            JsonNode body = JSON.readTree(exchange.getRequestBody().readAllBytes());
            Recorded recorded = new Recorded(exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization"), body);
            requests.add(recorded);
            Reply reply = handler.apply(recorded);
            reply.headers().forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(reply.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private VoyageEmbeddingModel model(int maxRequestTokens, int maxAttempts) {
        return new VoyageEmbeddingModel("http://127.0.0.1:" + server.getAddress().getPort(), "test-key", "voyage-4",
                DIM, maxRequestTokens, maxAttempts, sleeps::add);
    }

    // 입력 "t7" -> 벡터 [7,7,7,7], data 배열은 일부러 거꾸로 -> index 로 제자리를 찾는지 본다
    private static Reply echoReversed(Recorded request) {
        List<String> data = new ArrayList<>();
        JsonNode input = request.body().get("input");
        for (int i = 0; i < input.size(); i++) {
            int n = Integer.parseInt(input.get(i).asText().substring(1));
            data.add("{\"object\":\"embedding\",\"index\":" + i + ",\"embedding\":[" + n + "," + n + "," + n + "," + n + "]}");
        }
        Collections.reverse(data);
        return new Reply(200, Map.of(), "{\"object\":\"list\",\"model\":\"voyage-4\",\"data\":[" + String.join(",", data)
                + "],\"usage\":{\"total_tokens\":9}}");
    }

    private static List<Document> docs(int count) {
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            docs.add(new Document("t" + i));
        }
        return docs;
    }

    @Test
    @DisplayName("문서 임베딩은 /v1/embeddings 에 Bearer 키, 모델, 차원, input_type document 로 요청한다")
    void embedDocuments_요청모양_document() {
        List<float[]> result = model(100_000, 3).embed(docs(2), null, new TokenCountBatchingStrategy());

        assertEquals(1, requests.size());
        Recorded request = requests.get(0);
        assertEquals("/v1/embeddings", request.path());
        assertEquals("Bearer test-key", request.auth());
        assertEquals("voyage-4", request.body().get("model").asText());
        assertEquals(DIM, request.body().get("output_dimension").asInt());
        assertEquals("document", request.body().get("input_type").asText());
        assertEquals(2, request.body().get("input").size());
        assertArrayEquals(new float[] {0, 0, 0, 0}, result.get(0));
        assertArrayEquals(new float[] {1, 1, 1, 1}, result.get(1));
    }

    // PgVectorStore 는 검색어를 embed(String) 으로 부른다 -> 문서와 입력 유형이 달라야 검색 품질이 나온다
    @Test
    @DisplayName("검색어 임베딩은 input_type query 로 요청한다")
    void embedQuery_input_type_query() {
        float[] vector = model(100_000, 3).embed("t5");

        assertEquals("query", requests.get(0).body().get("input_type").asText());
        assertArrayEquals(new float[] {5, 5, 5, 5}, vector);
    }

    @Test
    @DisplayName("한 요청에 입력 1,000개를 넘기지 않고 나눠 보내며 결과는 입력 순서로 돌아온다")
    void embedDocuments_1000개씩분할_입력순서복원() {
        List<float[]> result = model(10_000_000, 3).embed(docs(2_500), null, new TokenCountBatchingStrategy());

        assertEquals(List.of(1000, 1000, 500), requests.stream().map(r -> r.body().get("input").size()).toList());
        assertEquals(2_500, result.size());
        for (int i = 0; i < result.size(); i++) {
            assertEquals(i, (int) result.get(i)[0], "입력 " + i + " 의 벡터가 제자리에 오지 않음");
        }
    }

    // 문자 수로 토큰을 넉넉히 센다(글자 1개 = 토큰 1개) -> 상한 90 이면 글자 40개짜리 입력은 요청당 2개
    @Test
    @DisplayName("요청의 추정 토큰이 상한을 넘지 않도록 입력을 나눈다")
    void embedDocuments_토큰상한분할() {
        List<Document> docs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            docs.add(new Document("t" + i + "x".repeat(38)));
        }
        handler = request -> echoReversed(stripTail(request));

        model(90, 3).embed(docs, null, new TokenCountBatchingStrategy());

        assertEquals(List.of(2, 2, 1), requests.stream().map(r -> r.body().get("input").size()).toList());
    }

    // 스텁은 "t<n>" 만 읽는다 -> 뒤에 붙인 채움 글자를 떼어낸 입력으로 응답을 만든다
    private static Recorded stripTail(Recorded request) {
        var copy = JSON.createObjectNode();
        var input = copy.putArray("input");
        request.body().get("input").forEach(n -> input.add(n.asText().replaceAll("x+$", "")));
        return new Recorded(request.path(), request.auth(), copy);
    }

    @Test
    @DisplayName("429 는 Retry-After 초만큼 기다렸다가 다시 보내고 성공하면 결과를 돌려준다")
    void embed_429_RetryAfter만큼대기후재시도() {
        int[] calls = {0};
        handler = request -> calls[0]++ == 0
                ? new Reply(429, Map.of("Retry-After", "3"), "{\"detail\":\"rate limit\"}")
                : echoReversed(request);

        float[] vector = model(100_000, 5).embed("t2");

        assertEquals(2, requests.size());
        assertEquals(List.of(3_000L), sleeps);
        assertArrayEquals(new float[] {2, 2, 2, 2}, vector);
    }

    @Test
    @DisplayName("Retry-After 가 없으면 1초부터 두 배씩 늘려 기다리고 60초를 넘기지 않는다")
    void embed_429_헤더없음_지수대기_60초상한() {
        int[] calls = {0};
        handler = request -> calls[0]++ < 8
                ? new Reply(429, Map.of(), "{\"detail\":\"rate limit\"}")
                : echoReversed(request);

        model(100_000, 10).embed("t1");

        assertEquals(List.of(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 32_000L, 60_000L, 60_000L), sleeps);
    }

    @Test
    @DisplayName("429 가 설정한 횟수만큼 이어지면 상태 코드를 담은 오류로 포기한다")
    void embed_429계속_횟수소진후오류() {
        handler = request -> new Reply(429, Map.of(), "{\"detail\":\"rate limit\"}");

        VoyageEmbeddingException error = assertThrows(VoyageEmbeddingException.class, () -> model(100_000, 5).embed("t1"));

        assertEquals(5, requests.size());
        assertEquals(4, sleeps.size());
        assertTrue(error.getMessage().contains("429"), error.getMessage());
    }

    @Test
    @DisplayName("429 가 아닌 오류 응답은 재시도 없이 바로 오류로 돌려준다")
    void embed_401_재시도없이오류() {
        handler = request -> new Reply(401, Map.of(), "{\"detail\":\"bad key\"}");

        VoyageEmbeddingException error = assertThrows(VoyageEmbeddingException.class, () -> model(100_000, 5).embed("t1"));

        assertEquals(1, requests.size());
        assertTrue(sleeps.isEmpty());
        assertTrue(error.getMessage().contains("401"), error.getMessage());
    }

    @Test
    @DisplayName("돌아온 벡터 길이가 설정한 차원과 다르면 오류다")
    void embed_차원불일치_오류() {
        handler = request -> new Reply(200, Map.of(),
                "{\"data\":[{\"index\":0,\"embedding\":[1,2,3]}],\"usage\":{\"total_tokens\":1}}");

        VoyageEmbeddingException error = assertThrows(VoyageEmbeddingException.class, () -> model(100_000, 3).embed("t1"));

        assertTrue(error.getMessage().contains("3") && error.getMessage().contains(String.valueOf(DIM)), error.getMessage());
    }

    @Test
    @DisplayName("입력 수와 응답 data 수가 다르면 오류다")
    void embed_응답개수불일치_오류() {
        handler = request -> new Reply(200, Map.of(), "{\"data\":[],\"usage\":{\"total_tokens\":1}}");

        assertThrows(VoyageEmbeddingException.class, () -> model(100_000, 3).embed("t1"));
    }

    @Test
    @DisplayName("차원은 설정값을 그대로 알려 호출 없이 VectorStore 가 쓸 수 있다")
    void dimensions_설정값_호출없음() {
        assertEquals(DIM, model(100_000, 3).dimensions());
        assertTrue(requests.isEmpty());
    }
}
