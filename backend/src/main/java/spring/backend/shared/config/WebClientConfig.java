package spring.backend.shared.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

// 완성품 대신 Builder 를 빈으로 노출 -> 호출처마다 baseUrl·헤더 등 다르게 커스터마이즈 가능
// WebClient -> 논블로킹 HTTP 클라이언트, LLM 등 외부 호출 시 스레드 점유 없이 처리
// 타임아웃 값을 생성자로 받는 이유 -> 설정이 실제로 동작하는지 단위 테스트로 확인 가능해짐
@Configuration
public class WebClientConfig {

  private final Duration connectTimeout;
  private final Duration responseTimeout;

  public WebClientConfig(
          @Value("${app.llm.connect-timeout:5s}") Duration connectTimeout,
          // responseTimeout 은 총 응답 시간이 아니라 read 사이의 최대 간격
          // 토큰이 계속 도착하는 정상 스트리밍은 끊기지 않고 상류가 멈춘 경우만 끊긴다
          @Value("${app.llm.response-timeout:20s}") Duration responseTimeout
  ) {
    this.connectTimeout = connectTimeout;
    this.responseTimeout = responseTimeout;
  }

  @Bean
  public WebClient.Builder webClientBuilder() {
    HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) connectTimeout.toMillis())
            .responseTimeout(responseTimeout);

    return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient));
  }
}
