package spring.backend.domain.auth.model.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// 계정 잠금 정책은 고정 창(fixed window) 이다
// 잠금 중 시도가 카운터와 lockedUntil 을 갱신하면 슬라이딩이 되어 계속 시도하는 동안 영구히 풀리지 않는다
// 실측(2026-07-30)에서 10초 간격 12회 시도 후 121초가 지나도 423 이었고 카운터가 19 까지 올라갔다
class CredentialsTest {

  private static final int MAX_ATTEMPTS = 5;
  private static final int LOCK_MINUTES = 1;

  private Credentials newCredentials() {
    return Credentials.createFormCredentials(null, "lock@test.com", "encoded");
  }

  @Test
  @DisplayName("최대 실패 횟수에 도달하면 계정이 잠긴다")
  void recordLoginFailure_reachesMaxAttempts_locks() {
    Credentials credentials = newCredentials();

    for (int i = 0; i < MAX_ATTEMPTS; i++) {
      credentials.recordLoginFailure(MAX_ATTEMPTS, LOCK_MINUTES);
    }

    assertTrue(credentials.isAccountLocked());
    assertEquals(MAX_ATTEMPTS, credentials.getFailedLoginAttempts());
  }

  @Test
  @DisplayName("최대 횟수 직전까지는 잠기지 않는다")
  void recordLoginFailure_belowMaxAttempts_notLocked() {
    Credentials credentials = newCredentials();

    for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
      credentials.recordLoginFailure(MAX_ATTEMPTS, LOCK_MINUTES);
    }

    assertFalse(credentials.isAccountLocked());
    assertEquals(MAX_ATTEMPTS - 1, credentials.getFailedLoginAttempts());
  }

  // 회귀 방지 핵심 1 - 잠금 중 시도가 카운터를 올리면 안 된다
  @Test
  @DisplayName("잠금 중 추가 실패는 실패 카운터를 올리지 않는다")
  void recordLoginFailure_whileLocked_doesNotIncrementCounter() {
    Credentials credentials = lockedCredentials();

    for (int i = 0; i < 7; i++) {
      credentials.recordLoginFailure(MAX_ATTEMPTS, LOCK_MINUTES);
    }

    assertEquals(MAX_ATTEMPTS, credentials.getFailedLoginAttempts());
  }

  // 회귀 방지 핵심 2 - 잠금 해제 시각이 뒤로 밀리면 안 된다
  @Test
  @DisplayName("잠금 중 추가 실패는 잠금 해제 시각을 미루지 않는다")
  void recordLoginFailure_whileLocked_doesNotExtendLockedUntil() {
    Credentials credentials = lockedCredentials();
    LocalDateTime lockedUntilBefore = credentials.getLockedUntil();

    credentials.recordLoginFailure(MAX_ATTEMPTS, LOCK_MINUTES);

    assertEquals(lockedUntilBefore, credentials.getLockedUntil());
  }

  // 잠금이 지나면 isAccountLocked() 가 스스로 해제하고 카운터를 초기화한다
  // 따라서 만료 후 첫 실패는 1회로 집계돼야 한다 (누적이 아니라 새 창)
  @Test
  @DisplayName("잠금이 만료되면 카운터가 초기화되고 다음 실패가 1회로 집계된다")
  void recordLoginFailure_afterLockExpires_startsNewWindow() {
    Credentials credentials = lockedCredentials();
    ReflectionTestUtils.setField(credentials, "lockedUntil", LocalDateTime.now().minusSeconds(1));

    credentials.recordLoginFailure(MAX_ATTEMPTS, LOCK_MINUTES);

    assertFalse(credentials.isAccountLocked());
    assertEquals(1, credentials.getFailedLoginAttempts());
  }

  @Test
  @DisplayName("로그인 성공 시 실패 카운터가 초기화된다")
  void recordLoginSuccess_resetsFailedAttempts() {
    Credentials credentials = newCredentials();
    credentials.recordLoginFailure(MAX_ATTEMPTS, LOCK_MINUTES);
    credentials.recordLoginFailure(MAX_ATTEMPTS, LOCK_MINUTES);

    credentials.recordLoginSuccess();

    assertEquals(0, credentials.getFailedLoginAttempts());
    assertFalse(credentials.isAccountLocked());
  }

  private Credentials lockedCredentials() {
    Credentials credentials = newCredentials();
    for (int i = 0; i < MAX_ATTEMPTS; i++) {
      credentials.recordLoginFailure(MAX_ATTEMPTS, LOCK_MINUTES);
    }
    return credentials;
  }
}
