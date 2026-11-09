package spring.backend.domain.chat.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import spring.backend.domain.chat.util.PromptLoader;

// llm-summary 는 ignoreExceptions 에 BusinessException 을 두는데 summaryChat 이 모든 실패를 BusinessException 으로 감싸면
// 서킷이 실패를 한 건도 세지 못해 영원히 닫혀 있다 -> 설정 파일의 목록을 읽어 던진 예외와 직접 대조한다
class LlmSummaryBreakerTest {

    @Test
    @DisplayName("RAG 서비스의 요약 실패는 llm-summary 무시 목록에 없는 예외로 던져진다")
    void summaryChat_업스트림실패_무시목록밖예외() throws Exception {
        ChatClient.Builder builder = failingClientBuilder();
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
        ChatClient.Builder builder = failingClientBuilder();
        ChatHistoryProvider history = mock(ChatHistoryProvider.class);
        when(history.getFullHistory("1")).thenReturn(List.of());
        PromptLoader promptLoader = mock(PromptLoader.class);
        when(promptLoader.buildChatSummaryPrompt()).thenReturn("요약해");

        LlmService service = new OpenAiLlmServiceWithoutRag(builder, history, promptLoader);

        assertCountedByBreaker(service);
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
    private ChatClient.Builder failingClientBuilder() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(client.prompt().system(any(String.class)).messages(anyList()).call().content())
                .thenThrow(new IllegalStateException("OpenAI 502"));
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
