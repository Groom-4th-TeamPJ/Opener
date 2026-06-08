package spring.backend.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

// 완성품 대신 Builder 를 빈으로 노출 -> 호출처마다 baseUrl·헤더 등 다르게 커스터마이즈 가능
// WebClient -> 논블로킹 HTTP 클라이언트, LLM 등 외부 호출 시 스레드 점유 없이 처리
@Configuration
public class WebClientConfig {

  @Bean
  public WebClient.Builder webClientBuilder() {
    return WebClient.builder();
  }
}
