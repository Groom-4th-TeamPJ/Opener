package spring.backend.domain.auth.model.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.backend.domain.auth.model.enums.Provider;
import spring.backend.domain.user.model.entity.User;
import spring.backend.shared.entity.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Credentials extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  @JoinColumn(nullable = false, unique = true)
  @Setter
  private User user;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Provider provider;

  @Column(unique = true)
  private String providerId;

  @Column(unique = true)
  private String email;

  @Column
  private String password;

  @Column
  private Timestamp lastLoginAt;

  @Column
  private Integer failedLoginAttempts = 0;

  @Column
  private LocalDateTime lastFailedLoginAt;

  @Column
  private LocalDateTime lockedUntil;

  public static Credentials createFormCredentials(
          User user,
          String email,
          String password
  ) {
    Credentials credentials = new Credentials();
    credentials.user = user;
    credentials.provider = Provider.FORM;
    credentials.email = email;
    credentials.password = password;
    return credentials;
  }

  public static Credentials createOAuthCredentials(
          User user,
          Provider provider,
          String providerId
  ) {
    Credentials credentials = new Credentials();
    credentials.user = user;
    credentials.provider = provider;
    credentials.providerId = providerId;
    return credentials;
  }

  // 로그인 성공 처리 - lastLoginAt 업데이트 - 실패 카운트 리셋
  public void recordLoginSuccess() {
    this.lastLoginAt = Timestamp.valueOf(LocalDateTime.now());
    this.resetFailedAttempts();
  }

  // 로그인 실패 처리 - 실패 카운트 증가 - lastFailedLoginAt 업데이트 - 최대 실패 횟수 도달 시 계정 잠금
  public void recordLoginFailure(int maxAttempts, int lockDurationMinutes) {
    // 잠금 중 시도는 집계하지 않는다 -> 카운터와 해제 시각을 갱신하면 잠금이 슬라이딩해 영구히 안 풀린다
    // isAccountLocked() 는 만료된 잠금을 스스로 해제하고 카운터를 초기화하므로 고정 창(fixed window) 이 된다
    if (this.isAccountLocked()) {
      return;
    }

    this.failedLoginAttempts++;
    this.lastFailedLoginAt = LocalDateTime.now();

    // 최대 실패 횟수 도달 시 계정 잠금
    if (this.failedLoginAttempts >= maxAttempts) {
      this.lockedUntil = LocalDateTime.now().plusMinutes(lockDurationMinutes);
    }
  }

  //계정 잠금 상태 확인 - lockedUntil이 null이면 잠금 안됨 - lockedUntil이 현재 시간보다 이전이면 잠금 자동 해제
  public boolean isAccountLocked() {
    if (this.lockedUntil == null) {
      return false;
    }

    // 잠금 시간이 지났으면 자동 해제
    if (LocalDateTime.now().isAfter(this.lockedUntil)) {
      this.unlockAccount();
      return false;
    }

    return true;
  }

  // 계정 잠금 해제 및 실패 카운트 초기화
  private void unlockAccount() {
    this.lockedUntil = null;
    this.resetFailedAttempts();
  }

  // 실패 카운트 초기화
  private void resetFailedAttempts() {
    this.failedLoginAttempts = 0;
    this.lastFailedLoginAt = null;
  }

  // 잠금 해제까지 남은 시간(분) 계산
  public long getRemainingLockTimeMinutes() {
    if (this.lockedUntil == null || LocalDateTime.now().isAfter(this.lockedUntil)) {
      return 0;
    }
    return java.time.Duration.between(LocalDateTime.now(), this.lockedUntil).toMinutes();
  }
}
