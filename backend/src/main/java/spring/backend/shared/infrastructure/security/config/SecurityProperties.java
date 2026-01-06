package spring.backend.shared.infrastructure.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// 보안 관련 설정 프로퍼티 application.yml의 security.* 설정을 바인딩
@Configuration
@ConfigurationProperties(prefix = "security")
@Getter
@Setter
public class SecurityProperties {

  private AccountLock accountLock = new AccountLock();

  @Getter
  @Setter
  public static class AccountLock {

    // 최대 로그인 실패 허용 횟수
    private int maxAttempts = 5;

    // 계정 잠금 지속 시간(분)
    private int lockDurationMinutes = 5;
  }
}
