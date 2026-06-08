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

// UserDetailsService 구현 -> Spring Security 표준 확장점, 인증 필터가 이 메서드로 사용자/비번을 가져감
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final CredentialRepository credentialRepository;

    // @Transactional -> credential.getUser().getRole() 지연 로딩 접근까지 영속성 컨텍스트 유지
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) {

        Credentials credential = credentialRepository
                .findUserCredentialByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        // 비밀번호 매칭 전에 잠금 먼저 차단 -> 잠긴 계정에 무차별 대입 시도 자체를 무력화
        if (credential.isAccountLocked()) {
            long remainingMinutes = credential.getRemainingLockTimeMinutes();
            log.warn("계정이 잠겼습니다. : " + email);
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // 비번 검증/권한 판단은 직접 안 함 -> UserDetails 만 넘기면 Security 가 표준 절차로 처리
        return org.springframework.security.core.userdetails.User
                .withUsername(credential.getEmail())
                .password(credential.getPassword())
                .roles(credential.getUser().getRole().toString())
                .build();
    }
}
