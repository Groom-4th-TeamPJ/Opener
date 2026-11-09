package spring.backend.domain.chat.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.yaml.snakeyaml.Yaml;
import spring.backend.domain.chat.service.spec.ChatHistoryProvider;
import spring.backend.domain.chat.service.spec.LlmService;
import org.springframework.test.util.ReflectionTestUtils;
import spring.backend.domain.chat.util.PromptLoader;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

// llm-summary 는 ignoreExceptions 에 BusinessException 을 두는데 summaryChat 이 모든 실패를 BusinessException 으로 감싸면
// 서킷이 실패를 한 건도 세지 못해 영원히 닫혀 있다 -> 설정 파일의 목록을 읽어 던진 예외와 직접 대조한다
class LlmSummaryBreakerTest {

    @Test
    @DisplayName("RAG 서비스의 요약 실패는 llm-summary 무시 목록에 없는 예외로 던져진다")
    void summaryChat_업스트림실패_무시목록밖예외() throws Exception {
        ChatClient.Builder builder = failingClientBuilder(new IllegalStateException("OpenAI 502"));
        ChatHistoryProvider history = mock(ChatHistoryProvider.class);
        when(history.getFullHistory("1")).thenReturn(List.of());
        PromptLoader promptLoader = mock(PromptLoader.class);
        when(promptLoader.buildChatSummaryPrompt()).thenReturn("요약해");

        LlmService service = new OpenAiLlmService(builder, history, mock(VectorStore.class), promptLoader,
                new SimpleMeterRegistry());

        assertCountedByBreaker(service);
    }

    @Test
    @DisplayName("RAG 없는 서비스의 요약 실패도 llm-summary 무시 목록에 없는 예외로 던져진다")
    void summaryChat_RAG없음_업스트림실패_무시목록밖예외() throws Exception {
        ChatClient.Builder builder = failingClientBuilder(new IllegalStateException("OpenAI 502"));
        ChatHistoryProvider history = mock(ChatHistoryProvider.class);
        when(history.getFullHistory("1")).thenReturn(List.of());
        PromptLoader promptLoader = mock(PromptLoader.class);
        when(promptLoader.buildChatSummaryPrompt()).thenReturn("요약해");

        LlmService service = new OpenAiLlmServiceWithoutRag(builder, history, promptLoader);

        assertCountedByBreaker(service);
    }


    // 히스토리 조회는 Redis 일 -> 실패가 llm-summary 서킷에 집계되거나 저장 전체를 막으면 안 되고 "요약 없이 저장" 으로 내려간다
    @Test
    @DisplayName("히스토리 조회가 MESSAGE_INPUT_FAIL 로 실패하면 요약 없이 null 을 돌려준다")
    void summaryChat_히스토리조회실패_null반환() {
        for (LlmService service : services(failingClientBuilder(new IllegalStateException("호출되면 안 됨")),
                new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL))) {
            assertNull(service.summaryChat("1"));
        }
    }

    // LLM 호출 중 던져진 도메인 거절은 감싸지 않고 올린다 -> 호출부가 입력 오류와 업스트림 장애를 구분한다
    @Test
    @DisplayName("LLM 호출 경로의 BusinessException 은 그대로 다시 던져진다")
    void summaryChat_도메인거절_그대로던짐() {
        BusinessException rejection = new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);
        for (LlmService service : services(failingClientBuilder(rejection), null)) {
            assertSame(rejection, assertThrows(BusinessException.class, () -> service.summaryChat("1")));
        }
    }

    @Test
    @DisplayName("서킷이 열려 호출이 거절되면 폴백이 null 을 돌려준다")
    void summaryChatFallback_서킷OPEN_null반환() {
        CircuitBreaker open = CircuitBreaker.ofDefaults("llm-summary");
        open.transitionToOpenState();
        CallNotPermittedException notPermitted = CallNotPermittedException.createCallNotPermittedException(open);

        for (LlmService service : services(failingClientBuilder(new IllegalStateException("x")), null)) {
            assertNull(ReflectionTestUtils.invokeMethod(service, "summaryChatFallback", "1", notPermitted));
        }
    }

    @Test
    @DisplayName("폴백은 업스트림 실패는 null 로 흡수하고 도메인 거절은 다시 던진다")
    void summaryChatFallback_업스트림은흡수_도메인거절은재던짐() {
        BusinessException rejection = new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        for (LlmService service : services(failingClientBuilder(new IllegalStateException("x")), null)) {
            assertNull(ReflectionTestUtils.invokeMethod(service, "summaryChatFallback", "1",
                    new LlmUpstreamException("호출 실패", new IllegalStateException("OpenAI 502"))));
            assertSame(rejection, assertThrows(BusinessException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "summaryChatFallback", "1", rejection)));
        }
    }

    // 두 구현을 같은 조건으로 돌린다 -> historyFailure 가 null 이면 히스토리는 빈 목록
    private List<LlmService> services(ChatClient.Builder builder, BusinessException historyFailure) {
        List<LlmService> result = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ChatHistoryProvider history = mock(ChatHistoryProvider.class);
            if (historyFailure == null) {
                when(history.getFullHistory("1")).thenReturn(List.of());
            } else {
                when(history.getFullHistory("1")).thenThrow(historyFailure);
            }
            PromptLoader promptLoader = mock(PromptLoader.class);
            when(promptLoader.buildChatSummaryPrompt()).thenReturn("요약해");
            result.add(i == 0
                    ? new OpenAiLlmService(builder, history, mock(VectorStore.class), promptLoader,
                            new SimpleMeterRegistry())
                    : new OpenAiLlmServiceWithoutRag(builder, history, promptLoader));
        }
        return result;
    }

    private void assertCountedByBreaker(LlmService service) throws Exception {
        List<Class<?>> ignored = loadSummaryIgnoreExceptions();
        assertFalse(ignored.isEmpty(), "llm-summary 의 ignoreExceptions 를 읽지 못함");

        Throwable thrown = null;
        try {
            service.summaryChat("1");
        } catch (Throwable t) {
            thrown = t;
        }
        assertNotNull(thrown, "업스트림 실패인데 summaryChat 이 예외를 던지지 않음");

        for (Class<?> ignoredType : ignored) {
            assertFalse(ignoredType.isAssignableFrom(thrown.getClass()),
                    thrown.getClass().getName() + " 은 무시 목록의 " + ignoredType.getName() + " 에 걸려 서킷이 집계하지 않는다");
        }

        // application.yml 과 같은 무시 목록, recordExceptions 기본값 -> 서킷이 실제로 실패로 센다
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .ignoreExceptions(ignored.toArray(new Class[0]))
                .build();
        assertTrue(config.getRecordExceptionPredicate().test(thrown));
    }

    // call() 단계에서 업스트림이 터지는 상황 -> 깊은 스텁으로 prompt().system().messages().call().content() 체인을 한 번에 만든다
    private ChatClient.Builder failingClientBuilder(Throwable failure) {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().system(any(String.class)).messages(anyList()).call().content())
                .thenThrow(failure);
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(client);
        return builder;
    }

    @SuppressWarnings("unchecked")
    private List<Class<?>> loadSummaryIgnoreExceptions() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            assertNotNull(in, "classpath 에 application.yml 없음");
            Map<String, Object> root = new Yaml().load(in);
            Map<String, Object> instance = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>)
                    ((Map<String, Object>) root.get("resilience4j")).get("circuitbreaker")).get("instances"))
                    .get("llm-summary");
            List<String> names = (List<String>) instance.get("ignoreExceptions");
            List<Class<?>> classes = new ArrayList<>();
            if (names != null) {
                for (String name : names) {
                    classes.add(Class.forName(name));
                }
            }
            return classes;
        }
    }
}
