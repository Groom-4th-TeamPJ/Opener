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
  private LocalDateTime LockedUntil;

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

  public void loginStamp() {
    this.lastLoginAt = Timestamp.valueOf(LocalDateTime.now());
  }

  // 로그인 실패시 failedLoginAttempts 1 증가
  public void failedLoginAttempts() {
    this.failedLoginAttempts++;
  }

  public void successLogin() {
    this.lastFailedLoginAt = LocalDateTime.now();
  }
}
