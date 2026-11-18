package spring.backend;

import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import reactor.core.scheduler.Schedulers;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan
public class BackendApplication {

  // Spring AI 의 Claude 스트림은 블로킹 SDK 스트림을 boundedElastic 에 올려 읽어 스트림이 끝날 때까지 자리 하나를 쥔다
  // 기본 상한(CPU 수 x 10)이면 동시 스트림이 그 수에서 줄을 선다(r129: 자리 100 x 스트림 9.5초 -> 초당 약 10개), 자리는 가상 스레드라 크게 잡는다
  // 스트림 하나가 Claude 읽기와 publishOn 콜백으로 자리 2개를 쓸 수 있어 동시 1,000 세션의 두 배에 여유를 둔다
  static final String BOUNDED_ELASTIC_SIZE = "reactor.schedulers.defaultBoundedElasticSize";
  static final String DEFAULT_BOUNDED_ELASTIC_SIZE = "4000";

  public static void main(String[] args) {
    raiseBoundedElasticCap();
    SpringApplication.run(BackendApplication.class, args);
    // 무언가 Schedulers 를 먼저 읽었다면 설정이 무시된다 -> 실제로 쓰이는 값을 남긴다
    LoggerFactory.getLogger(BackendApplication.class)
            .info("[Reactor] boundedElastic 상한 {}", Schedulers.DEFAULT_BOUNDED_ELASTIC_SIZE);
  }

  // Reactor 가 Schedulers 를 처음 읽기 전에 불러야 한다, -D 로 준 값은 그대로 둔다
  static void raiseBoundedElasticCap() {
    if (System.getProperty(BOUNDED_ELASTIC_SIZE) == null) {
      System.setProperty(BOUNDED_ELASTIC_SIZE, DEFAULT_BOUNDED_ELASTIC_SIZE);
    }
  }

}