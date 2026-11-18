package spring.backend.domain.chat.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptions;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Voyage AI 임베딩 (Spring AI 에 Voyage 모듈이 없어 RestClient 로 직접 호출)
 *
 * <p>PgVectorStore 는 문서를 embed(List&lt;Document&gt;, options, strategy) 로, 검색어를 embed(String) 으로 넣는다 (2.0.2 소스 확인)
 * -> 앞쪽은 input_type "document", 뒤쪽은 "query" 로 보낸다. call(EmbeddingRequest) 은 유형을 모르므로 input_type 을 생략한다.
 */
@Slf4j
public class VoyageEmbeddingModel implements EmbeddingModel {

    static final int MAX_INPUTS_PER_REQUEST = 1_000;
    private static final long MAX_WAIT_MILLIS = 60_000;
    private static final long FIRST_WAIT_MILLIS = 1_000;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient restClient;
    private final String model;
    private final int outputDimension;
    private final int maxRequestTokens;
    private final int maxAttempts;
    private final LongConsumer sleeper;

    public VoyageEmbeddingModel(String baseUrl, String apiKey, String model, int outputDimension,
                                int maxRequestTokens, int maxAttempts, LongConsumer sleeper) {
        // HTTP/2 업그레이드 시도를 끈다 -> 평문 서버(로컬 스텁·프록시)에서도 같은 방식으로 동작
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.model = model;
        this.outputDimension = outputDimension;
        this.maxRequestTokens = maxRequestTokens;
        this.maxAttempts = maxAttempts;
        this.sleeper = sleeper;
    }

    // 모델별 요청당 토큰 상한(문서 기준)의 80% -> 추정 오차 여유, 모르는 모델은 가장 작은 상한으로 본다
    public static int defaultMaxRequestTokens(String model) {
        int cap = switch (model) {
            case "voyage-4" -> 320_000;
            case "voyage-4-lite" -> 1_000_000;
            default -> 120_000;
        };
        return cap / 5 * 4;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<float[]> vectors = embedTexts(request.getInstructions(), null);
        List<Embedding> embeddings = new ArrayList<>(vectors.size());
        for (int i = 0; i < vectors.size(); i++) {
            embeddings.add(new Embedding(vectors.get(i), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(String text) {
        return embedTexts(List.of(text), "query").get(0);
    }

    @Override
    public float[] embed(Document document) {
        return embedTexts(List.of(contentOf(document)), "document").get(0);
    }

    // 배치 전략은 쓰지 않는다 -> 토큰 상한 8,191(OpenAI 기준)이라 Voyage 상한과 맞지 않고, 분할은 아래에서 직접 한다
    @Override
    public List<float[]> embed(List<Document> documents, EmbeddingOptions options, BatchingStrategy batchingStrategy) {
        return embedTexts(documents.stream().map(this::contentOf).toList(), "document");
    }

    @Override
    public int dimensions() {
        return outputDimension;
    }

    private String contentOf(Document document) {
        String text = getEmbeddingContent(document);
        if (text == null) {
            throw new VoyageEmbeddingException("텍스트가 없는 문서는 임베딩할 수 없음 - id: " + document.getId());
        }
        return text;
    }

    private List<float[]> embedTexts(List<String> texts, String inputType) {
        List<float[]> result = new ArrayList<>(texts.size());
        for (List<String> batch : split(texts)) {
            result.addAll(requestWithRetry(batch, inputType));
        }
        return result;
    }

    // 입력 1,000개 이하 + 추정 토큰 상한 이하 -> 글자 하나를 토큰 하나로 센다(한글·수식 기준 넉넉한 쪽)
    // 상한보다 큰 입력 하나는 혼자 한 요청으로 보낸다 -> Voyage 가 truncation 으로 자른다
    private List<List<String>> split(List<String> texts) {
        List<List<String>> batches = new ArrayList<>();
        List<String> current = new ArrayList<>();
        long tokens = 0;
        for (String text : texts) {
            long estimate = text.length();
            if (!current.isEmpty() && (current.size() >= MAX_INPUTS_PER_REQUEST || tokens + estimate > maxRequestTokens)) {
                batches.add(current);
                current = new ArrayList<>();
                tokens = 0;
            }
            current.add(text);
            tokens += estimate;
        }
        if (!current.isEmpty()) {
            batches.add(current);
        }
        return batches;
    }

    private List<float[]> requestWithRetry(List<String> batch, String inputType) {
        for (int attempt = 1; ; attempt++) {
            Raw raw = send(batch, inputType);
            if (raw.status() == 200) {
                return parse(raw.body(), batch.size());
            }
            if (raw.status() == 429 && attempt < maxAttempts) {
                long wait = waitMillis(raw.retryAfter(), attempt);
                log.warn("[Voyage] 429 - {}ms 뒤 재시도 ({}/{})", wait, attempt, maxAttempts);
                sleeper.accept(wait);
                continue;
            }
            String tail = raw.status() == 429 ? " (재시도 " + attempt + "회 소진)" : "";
            throw new VoyageEmbeddingException("Voyage 임베딩 호출 실패 - HTTP " + raw.status() + tail + ": "
                    + abbreviate(raw.body()));
        }
    }

    // Retry-After 초가 있으면 그만큼, 없으면 1초부터 두 배씩 -> 둘 다 60초를 넘기지 않는다
    private static long waitMillis(String retryAfter, int attempt) {
        if (retryAfter != null) {
            try {
                return Math.min(Math.max(Long.parseLong(retryAfter.trim()), 0) * 1_000, MAX_WAIT_MILLIS);
            } catch (NumberFormatException ignored) {
                // HTTP 날짜 형식은 쓰지 않는다 -> 지수 대기로 넘어간다
            }
        }
        return Math.min(FIRST_WAIT_MILLIS << Math.min(attempt - 1, 20), MAX_WAIT_MILLIS);
    }

    private Raw send(List<String> batch, String inputType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("input", batch);
        body.put("model", model);
        body.put("output_dimension", outputDimension);
        if (inputType != null) {
            body.put("input_type", inputType);
        }
        try {
            return restClient.post()
                    .uri("/v1/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(JSON.writeValueAsString(body))
                    .exchange((request, response) -> new Raw(response.getStatusCode().value(),
                            response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER),
                            new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
        } catch (RestClientException | java.io.IOException e) {
            throw new VoyageEmbeddingException("Voyage 임베딩 호출 실패 - 연결 오류: " + e.getClass().getSimpleName(), e);
        }
    }

    private List<float[]> parse(String body, int expectedCount) {
        JsonNode data;
        try {
            data = JSON.readTree(body).get("data");
        } catch (java.io.IOException e) {
            throw new VoyageEmbeddingException("Voyage 응답을 읽지 못함", e);
        }
        if (data == null || !data.isArray() || data.size() != expectedCount) {
            throw new VoyageEmbeddingException("Voyage 응답 개수 불일치 - 입력 " + expectedCount + "개, 응답 "
                    + (data == null ? "data 없음" : data.size() + "개"));
        }
        float[][] ordered = new float[expectedCount][];
        for (JsonNode item : data) {
            int index = item.path("index").asInt(-1);
            JsonNode embedding = item.path("embedding");
            if (index < 0 || index >= expectedCount || ordered[index] != null || !embedding.isArray()) {
                throw new VoyageEmbeddingException("Voyage 응답 index 가 올바르지 않음 - index: " + index);
            }
            if (embedding.size() != outputDimension) {
                throw new VoyageEmbeddingException("Voyage 벡터 길이 " + embedding.size() + " 이(가) 설정 차원 "
                        + outputDimension + " 와 다름");
            }
            float[] vector = new float[outputDimension];
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) embedding.get(i).asDouble();
            }
            ordered[index] = vector;
        }
        return List.of(ordered);
    }

    private static String abbreviate(String body) {
        return body.length() <= 200 ? body : body.substring(0, 200) + "...";
    }

    private record Raw(int status, String retryAfter, String body) {}
}
