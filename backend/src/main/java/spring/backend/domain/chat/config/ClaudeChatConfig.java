package spring.backend.domain.chat.config;

import com.anthropic.models.messages.OutputConfig;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 모델·최대 토큰·재시도는 spring.ai.anthropic.* 속성, effort 는 속성으로 바인딩할 수 없어 여기서 둔다
// 샘플링 값과 thinking 은 일부러 비운다 -> Sonnet 5.5 는 API 기본값(적응형 thinking)을 쓰고 샘플링 값을 받지 않을 수 있다
@Configuration
public class ClaudeChatConfig {

    // effort low -> 튜터링 채팅의 시작값, 측정으로 올릴지 정한다
    @Bean
    public ChatClientBuilderCustomizer claudeChatClientBuilderCustomizer() {
        return builder -> builder.defaultOptions(
                AnthropicChatOptions.builder().effort(OutputConfig.Effort.LOW));
    }
}
