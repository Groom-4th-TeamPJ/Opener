package spring.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan
public class BackendApplication {

  // Spring AI 의 Claude 스트림은 블로킹 SDK 스트림을 boundedElastic 에 올려 읽어 스트림이 끝날 때까지 자리 하나를 쥔다
  // 기본 상한(CPU 수 x 10)이면 동시 스트림이 그 수에서 줄을 선다(r129: 자리 100 x 스트림 9.5초 -> 초당 약 10개), 자리는 가상 스레드라 크게 잡는다
  static final String BOUNDED_ELASTIC_SIZE = "reactor.schedulers.defaultBoundedElasticSize";
  static final String DEFAULT_BOUNDED_ELASTIC_SIZE = "2000";

  public static void main(String[] args) {
    raiseBoundedElasticCap();
    SpringApplication.run(BackendApplication.class, args);
  }

  // Reactor 가 Schedulers 를 처음 읽기 전에 불러야 한다, -D 로 준 값은 그대로 둔다
  static void raiseBoundedElasticCap() {
    if (System.getProperty(BOUNDED_ELASTIC_SIZE) == null) {
      System.setProperty(BOUNDED_ELASTIC_SIZE, DEFAULT_BOUNDED_ELASTIC_SIZE);
    }
  }

}