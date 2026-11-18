package spring.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// main 이 Spring 기동 전에 Reactor boundedElastic 상한을 올리는지 - r129 에서 상한 100 이 Claude 동시 스트림을 막았다
class BackendApplicationBoundedElasticTest {

  private static final String KEY = "reactor.schedulers.defaultBoundedElasticSize";
  private String saved;

  @BeforeEach
  void save() {
    saved = System.getProperty(KEY);
    System.clearProperty(KEY);
  }

  @AfterEach
  void restore() {
    if (saved == null) {
      System.clearProperty(KEY);
    } else {
      System.setProperty(KEY, saved);
    }
  }

  @Test
  void raiseBoundedElasticCap_속성없음_4000으로설정한다() {
    BackendApplication.raiseBoundedElasticCap();

    assertEquals("4000", System.getProperty(KEY));
  }

  @Test
  void raiseBoundedElasticCap_명시값있음_그대로둔다() {
    System.setProperty(KEY, "300");

    BackendApplication.raiseBoundedElasticCap();

    assertEquals("300", System.getProperty(KEY));
  }
}
