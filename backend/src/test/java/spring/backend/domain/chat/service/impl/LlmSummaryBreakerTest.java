package spring.backend.domain.chat.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.vectorstore.VectorStore;
import org.yaml.snakeyaml.Yaml;
import spring.backend.domain.chat.service.spec.ChatHistoryProvider;
import spring.backend.domain.chat.service.spec.LlmService;
import org.springframework.test.util.ReflectionTestUtils;
import spring.backend.domain.chat.util.PromptLoader;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

// llm-summary 는 ignoreExceptions 에 BusinessException 을 두는데 summarize 가 모든 실패를 BusinessException 으로 감싸면
// 서킷이 실패를 한 건도 세지 못해 영원히 닫혀 있다 -> 설정 파일의 목록을 읽어 던진 예외와 직접 대조한다
class LlmSummaryBreakerTest {

    private static final List<Message> HISTORY = List.of(new UserMessage("질문"), new AssistantMessage("답"));

    @Test
    @DisplayName("두 구현의 요약 실패는 llm-summary 무시 목록에 없는 예외로 던져진다")
    void summarize_업스트림실패_무시목록밖예외() throws Exception {
        for (LlmService service : services(failingClientBuilder(new IllegalStateException("Anthropic 529")))) {
            assertCountedByBreaker(service);
        }
    }

    @Test
    @DisplayName("요약은 기존 요약 프롬프트를 시스템 메시지로, 받은 메시지를 대화로 넘겨 요약문을 돌려준다")
    void summarize_성공_요약프롬프트로호출해요약문반환() {
        ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        // 시스템 프롬프트가 "요약해" 일 때만 스텁이 걸린다 -> 다른 프롬프트로 부르면 null 이 나와 실패한다
        when(client.prompt().system("요약해").messages(HISTORY).call().content()).thenReturn("요약문");
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(client);

        for (LlmService service : services(builder)) {
            assertEquals("요약문", service.summarize(HISTORY));
        }
    }

    // 서킷 이름은 Spring AOP 가 있어야 동작한다 -> 단위 테스트에서는 어노테이션을 직접 읽는다
    @Test
    @DisplayName("두 구현의 summarize 는 llm-summary 서킷과 summarizeFallback 폴백 아래에 있다")
    void summarize_두구현_llmSummary서킷아래에있다() throws Exception {
        for (Class<?> type : List.of(RagLlmService.class, PlainLlmService.class)) {
            io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker breaker = type
                    .getMethod("summarize", List.class)
                    .getAnnotation(io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker.class);
            assertNotNull(breaker, type.getSimpleName() + ".summarize 에 서킷 어노테이션이 없음");
            assertEquals("llm-summary", breaker.name());
            assertEquals("summarizeFallback", breaker.fallbackMethod());
        }
    }

    // LLM 호출 중 던져진 도메인 거절은 감싸지 않고 올린다 -> 호출부가 입력 오류와 업스트림 장애를 구분한다
    @Test
    @DisplayName("LLM 호출 경로의 BusinessException 은 그대로 다시 던져진다")
    void summarize_도메인거절_그대로던짐() {
        BusinessException rejection = new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);
        for (LlmService service : services(failingClientBuilder(rejection))) {
            assertSame(rejection, assertThrows(BusinessException.class, () -> service.summarize(HISTORY)));
        }
    }

    @Test
    @DisplayName("서킷이 열려 호출이 거절되면 폴백이 null 을 돌려준다")
    void summarizeFallback_서킷OPEN_null반환() {
        CircuitBreaker open = CircuitBreaker.ofDefaults("llm-summary");
        open.transitionToOpenState();
        CallNotPermittedException notPermitted = CallNotPermittedException.createCallNotPermittedException(open);

        for (LlmService service : services(failingClientBuilder(new IllegalStateException("x")))) {
            assertNull(ReflectionTestUtils.invokeMethod(service, "summarizeFallback", HISTORY, notPermitted));
        }
    }

    @Test
    @DisplayName("폴백은 업스트림 실패는 null 로 흡수하고 도메인 거절은 다시 던진다")
    void summarizeFallback_업스트림은흡수_도메인거절은재던짐() {
        BusinessException rejection = new BusinessException(ErrorCode.MESSAGE_INPUT_FAIL);
        for (LlmService service : services(failingClientBuilder(new IllegalStateException("x")))) {
            assertNull(ReflectionTestUtils.invokeMethod(service, "summarizeFallback", HISTORY,
                    new LlmUpstreamException("호출 실패", new IllegalStateException("Anthropic 529"))));
            assertSame(rejection, assertThrows(BusinessException.class,
                    () -> ReflectionTestUtils.invokeMethod(service, "summarizeFallback", HISTORY, rejection)));
        }
    }

    // Anthropic SDK 는 429·5xx·연결 실패를 AnthropicException 계열 RuntimeException 으로 던진다 -> 무시 목록(BusinessException)에 걸리지 않고 서킷이 센다
    @Test
    @DisplayName("Anthropic SDK 예외로 요약이 실패하면 LlmUpstreamException 으로 감싸 서킷이 집계한다")
    void summarize_AnthropicSDK예외_서킷이집계() throws Exception {
        for (LlmService service : services(failingClientBuilder(new com.anthropic.errors.AnthropicException("rate limited")))) {
            assertCountedByBreaker(service);
            LlmUpstreamException thrown = assertThrows(LlmUpstreamException.class, () -> service.summarize(HISTORY));
            assertTrue(thrown.getCause() instanceof com.anthropic.errors.AnthropicException);
        }
    }

    // 거절 응답(stop_reason refusal)은 텍스트가 없다 -> 빈 요약이 저장되지 않게 null 로 돌려 소비자가 재시도·DLQ 로 보낸다
    // 대화 하나의 거절은 업스트림 장애가 아니므로 서킷에는 실패로 세지 않는다
    @Test
    @DisplayName("요약 응답이 비어 있으면 빈 문자열 대신 null 을 돌려준다")
    void summarize_빈응답_null반환() throws Exception {
        for (String empty : new String[] {null, "", "   "}) {
            ChatClient client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
            when(client.prompt().system("요약해").messages(HISTORY).call().content()).thenReturn(empty);
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            when(builder.build()).thenReturn(client);

            for (LlmService service : services(builder)) {
                assertNull(service.summarize(HISTORY));
            }
        }
    }

    // 두 구현을 같은 조건으로 돌린다
    private List<LlmService> services(ChatClient.Builder builder) {
        List<LlmService> result = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ChatHistoryProvider history = mock(ChatHistoryProvider.class);
            PromptLoader promptLoader = mock(PromptLoader.class);
            when(promptLoader.buildChatSummaryPrompt()).thenReturn("요약해");
            result.add(i == 0
                    ? new RagLlmService(builder, history, mock(VectorStore.class), promptLoader,
                            new SimpleMeterRegistry())
                    : new PlainLlmService(builder, history, promptLoader));
        }
        return result;
    }

    private void assertCountedByBreaker(LlmService service) throws Exception {
        List<Class<?>> ignored = loadSummaryIgnoreExceptions();
        assertFalse(ignored.isEmpty(), "llm-summary 의 ignoreExceptions 를 읽지 못함");

        Throwable thrown = null;
        try {
            service.summarize(HISTORY);
        } catch (Throwable t) {
            thrown = t;
        }
        assertNotNull(thrown, "업스트림 실패인데 summarize 가 예외를 던지지 않음");

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
