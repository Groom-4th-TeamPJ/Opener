package spring.backend.domain.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.auth.model.entity.Credentials;
import spring.backend.domain.auth.respository.spec.CredentialRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

  private final CredentialRepository credentialRepository;

  @Override
  @Transactional
  public UserDetails loadUserByUsername(String email) {

    Credentials credential = credentialRepository
            .findUserCredentialByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

    // 계정 잠금 상태 체크
    if (credential.isAccountLocked()) {
      long remainingMinutes = credential.getRemainingLockTimeMinutes();
      throw new LockedException(
              String.format("계정이 잠겼습니다. %d분 후 다시 시도해주세요.", remainingMinutes)
      );
    }

    return org.springframework.security.core.userdetails.User
            .withUsername(credential.getEmail())
            .password(credential.getPassword())
            .roles(credential.getUser().getRole().toString())
            .build();
  }
}
