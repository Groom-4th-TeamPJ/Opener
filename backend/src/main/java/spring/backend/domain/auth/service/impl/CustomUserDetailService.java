package spring.backend.domain.auth.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import spring.backend.domain.auth.model.entity.Credentials;
import spring.backend.domain.auth.respository.spec.CredentialRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

  private final CredentialRepository credentialRepository;

  @Override
  public UserDetails loadUserByUsername(String email) {

    Credentials credential = credentialRepository
            .findUserCredentialByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    return org.springframework.security.core.userdetails.User
            .withUsername(credential.getEmail())
            .password(credential.getPassword())
            .roles(credential.getUser().getRole().toString())
            .build();
  }
}
