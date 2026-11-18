package spring.backend.domain.chat.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.anthropic.models.messages.OutputConfig;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.yaml.snakeyaml.Yaml;
import spring.backend.domain.chat.config.ClaudeChatConfig;

// Claude Sonnet 5.5 로 채팅 -> 모델 · 최대 토큰 · 샘플링 값 미전송 · effort low · 재시도 1회가 설정과 코드에 실제로 들어 있는지 본다
class LlmServiceProviderConfigTest {

    @Test
    @DisplayName("dev 설정은 claude-sonnet-5-5, 최대 토큰 8000, 재시도 1회, 환경변수 키를 쓴다")
    void devYml_모델_최대토큰_재시도_키() throws Exception {
        Map<String, String> flat = flat("application-dev.yml");

        assertTrue(flat.get("spring.ai.anthropic.chat.model").contains("claude-sonnet-5-5"),
                flat.get("spring.ai.anthropic.chat.model"));
        assertEquals("8000", flat.get("spring.ai.anthropic.chat.max-tokens"));
        assertEquals("1", flat.get("spring.ai.anthropic.max-retries"));
        assertEquals("${ANTHROPIC_API_KEY}", flat.get("spring.ai.anthropic.api-key"));
    }

    // 샘플링 값은 보내지 않는다 -> Sonnet 5.5 는 temperature · top_p · top_k 를 받지 않을 수 있다
    @Test
    @DisplayName("Anthropic 설정에 샘플링 값(temperature · top-p · top-k)이 없다")
    void devYml_샘플링값없음() throws Exception {
        Map<String, String> flat = flat("application-dev.yml");

        flat.keySet().stream().filter(k -> k.startsWith("spring.ai.anthropic")).forEach(key ->
                assertFalse(key.matches(".*(temperature|top-p|top-k|topp|topk).*"), key));
    }

    @Test
    @DisplayName("설정 파일 어디에도 OpenAI 키가 남아 있지 않다")
    void yml_openai키없음() throws Exception {
        for (String file : new String[] {"application.yml", "application-dev.yml", "application-prod.yml"}) {
            flat(file).forEach((key, value) -> {
                assertFalse(key.contains("openai"), file + ": " + key);
                assertFalse(String.valueOf(value).toLowerCase().contains("openai"), file + ": " + key + "=" + value);
            });
        }
    }

    @Test
    @DisplayName("ChatClient 기본 옵션은 effort low 이고 샘플링 값과 thinking 은 비어 있다")
    void effort_low_샘플링값과thinking비어있음() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);

        new ClaudeChatConfig().claudeChatClientCustomizer().customize(builder);

        ArgumentCaptor<ChatOptions> options = ArgumentCaptor.forClass(ChatOptions.class);
        verify(builder).defaultOptions(options.capture());
        AnthropicChatOptions claude = (AnthropicChatOptions) options.getValue();
        assertNotNull(claude.getOutputConfig());
        assertEquals(OutputConfig.Effort.LOW, claude.getOutputConfig().effort().orElseThrow());
        assertNull(claude.getTemperature());
        assertNull(claude.getTopP());
        assertNull(claude.getTopK());
        assertNull(claude.getThinking());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> flat(String resource) throws Exception {
        try (InputStream in = LlmServiceProviderConfigTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, "classpath 에 " + resource + " 없음");
            Map<String, String> out = new LinkedHashMap<>();
            Object root = new Yaml().load(in);
            if (root instanceof Map<?, ?> map) {
                walk("", (Map<String, Object>) map, out);
            }
            return out;
        }
    }

    @SuppressWarnings("unchecked")
    private static void walk(String prefix, Map<String, Object> node, Map<String, String> out) {
        node.forEach((k, v) -> {
            String key = prefix.isEmpty() ? k : prefix + "." + k;
            if (v instanceof Map<?, ?> child) {
                walk(key, (Map<String, Object>) child, out);
            } else {
                out.put(key, String.valueOf(v));
            }
        });
    }
}
