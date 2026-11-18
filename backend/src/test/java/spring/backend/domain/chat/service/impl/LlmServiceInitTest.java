package spring.backend.domain.chat.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;
import spring.backend.domain.chat.service.spec.ChatHistoryProvider;
import spring.backend.domain.chat.util.PromptLoader;

// 짧은 키가 마지막 분기에서 substring(0, 10) 을 타면 StringIndexOutOfBoundsException 으로 기동이 죽는다
class LlmServiceInitTest {

    private static final String SHORT_KEY = "sk-ant-12";

    private RagLlmService rag() {
        return new RagLlmService(
                Mockito.mock(ChatClient.Builder.class),
                Mockito.mock(ChatHistoryProvider.class),
                Mockito.mock(VectorStore.class),
                Mockito.mock(PromptLoader.class),
                new SimpleMeterRegistry());
    }

    private PlainLlmService plain() {
        return new PlainLlmService(
                Mockito.mock(ChatClient.Builder.class),
                Mockito.mock(ChatHistoryProvider.class),
                Mockito.mock(PromptLoader.class));
    }

    @Test
    @DisplayName("RAG 서비스는 10자 미만 키에서도 기동 검증이 예외 없이 끝난다")
    void validateApiKeyBinding_짧은키_예외없음() {
        RagLlmService service = rag();
        ReflectionTestUtils.setField(service, "anthropicApiKey", SHORT_KEY);

        assertDoesNotThrow(service::validateApiKeyBinding);
    }

    @Test
    @DisplayName("RAG 없는 서비스는 10자 미만 키에서도 기동 검증이 예외 없이 끝난다")
    void validateApiKeyBinding_RAG없음_짧은키_예외없음() {
        PlainLlmService service = plain();
        ReflectionTestUtils.setField(service, "anthropicApiKey", SHORT_KEY);

        assertDoesNotThrow(service::validateApiKeyBinding);
    }

    @Test
    @DisplayName("키가 비었거나 OpenAI 형식(sk-)이어도 기동 검증은 던지지 않는다")
    void validateApiKeyBinding_빈키_OpenAI형식키_예외없음() {
        for (String key : new String[] {"", null, "sk-proj-abcdefghijklmnop", "${ANTHROPIC_API_KEY}"}) {
            RagLlmService rag = rag();
            PlainLlmService plain = plain();
            ReflectionTestUtils.setField(rag, "anthropicApiKey", key);
            ReflectionTestUtils.setField(plain, "anthropicApiKey", key);

            assertDoesNotThrow(rag::validateApiKeyBinding);
            assertDoesNotThrow(plain::validateApiKeyBinding);
        }
    }

    // 키 이름을 틀리면 검증이 영원히 "미설정" 으로만 찍힌다 -> 주입 식이 Spring AI Anthropic 의 실제 키를 읽는지 본다
    @Test
    @DisplayName("기동 검증은 spring.ai.anthropic.api-key 를 읽는다")
    void apiKey필드_주입식_anthropic키() throws Exception {
        for (Class<?> type : new Class<?>[] {RagLlmService.class, PlainLlmService.class}) {
            Field field = type.getDeclaredField("anthropicApiKey");
            Value value = field.getAnnotation(Value.class);
            assertNotNull(value, type.getSimpleName() + ".anthropicApiKey 에 @Value 없음");
            assertEquals("${spring.ai.anthropic.api-key:}", value.value());
        }
    }

    @Test
    @DisplayName("키 형식 안내는 Anthropic 키 접두어 sk-ant- 를 기준으로 한다")
    void 키접두어상수_skant() throws Exception {
        for (Class<?> type : new Class<?>[] {RagLlmService.class, PlainLlmService.class}) {
            Field prefix = type.getDeclaredField("KEY_PREFIX");
            prefix.setAccessible(true);
            assertTrue("sk-ant-".equals(prefix.get(null)));
        }
    }
}
