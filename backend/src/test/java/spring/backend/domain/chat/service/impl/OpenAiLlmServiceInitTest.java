package spring.backend.domain.chat.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;
import spring.backend.domain.chat.service.spec.ChatHistoryProvider;
import spring.backend.domain.chat.util.PromptLoader;

// "sk-" 로 시작하는 10자 미만 키가 마지막 분기에서 substring(0, 10) 을 타면 StringIndexOutOfBoundsException 으로 기동이 죽는다
class OpenAiLlmServiceInitTest {

    private static final String SHORT_KEY = "sk-12";

    @Test
    @DisplayName("RAG 서비스는 10자 미만 키에서도 기동 검증이 예외 없이 끝난다")
    void validateApiKeyBinding_짧은키_예외없음() {
        OpenAiLlmService service = new OpenAiLlmService(
                Mockito.mock(ChatClient.Builder.class),
                Mockito.mock(ChatHistoryProvider.class),
                Mockito.mock(VectorStore.class),
                Mockito.mock(PromptLoader.class),
                new SimpleMeterRegistry());
        ReflectionTestUtils.setField(service, "openaiApiKey", SHORT_KEY);

        assertDoesNotThrow(service::validateApiKeyBinding);
    }

    @Test
    @DisplayName("RAG 없는 서비스는 10자 미만 키에서도 기동 검증이 예외 없이 끝난다")
    void validateApiKeyBinding_RAG없음_짧은키_예외없음() {
        OpenAiLlmServiceWithoutRag service = new OpenAiLlmServiceWithoutRag(
                Mockito.mock(ChatClient.Builder.class),
                Mockito.mock(ChatHistoryProvider.class),
                Mockito.mock(PromptLoader.class));
        ReflectionTestUtils.setField(service, "openaiApiKey", SHORT_KEY);

        assertDoesNotThrow(service::validateApiKeyBinding);
    }
}
