package spring.backend.shared.config;

import java.net.ServerSocket;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

// 응답을 영영 주지 않는 서버를 세워, 설정한 타임아웃 안에 실제로 끊기는지 확인
// 설정 파일에 값을 적었다와 그 값이 실제로 동작한다는 다른 명제다
class WebClientConfigTest {

    @Test
    @DisplayName("응답이 오지 않으면 responseTimeout 안에 에러로 끊긴다")
    void webClient_무응답서버_응답타임아웃으로_실패한다() throws Exception {
        try (ServerSocket stalled = new ServerSocket(0)) {
            // accept 만 하고 아무것도 쓰지 않는 서버
            Thread accepter = Thread.ofVirtual().start(() -> {
                try {
                    stalled.accept();
                } catch (Exception ignored) {
                    // 테스트 종료 시 소켓이 닫히며 나는 예외라 무시
                }
            });

            WebClient client = new WebClientConfig(
                    Duration.ofMillis(500),
                    Duration.ofMillis(700)
            ).webClientBuilder().build();

            StepVerifier.create(
                            client.get()
                                    .uri("http://127.0.0.1:" + stalled.getLocalPort() + "/v1/chat/completions")
                                    .retrieve()
                                    .bodyToMono(String.class))
                    .expectError()
                    .verify(Duration.ofSeconds(5));

            accepter.interrupt();
        }
    }
}
