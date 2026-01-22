package spring.backend.domain.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.auth.model.entity.Credentials;
import spring.backend.domain.auth.respository.spec.CredentialRepository;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@Slf4j
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
            log.warn("계정이 잠겼습니다. : " + email);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(credential.getEmail())
                .password(credential.getPassword())
                .roles(credential.getUser().getRole().toString())
                .build();
    }
}
