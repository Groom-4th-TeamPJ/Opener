package spring.backend.domain.auth.respository.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import spring.backend.domain.auth.model.entity.Credentials;
import spring.backend.domain.auth.respository.jpa.JpaCredentialRepository;
import spring.backend.domain.auth.respository.spec.CredentialRepository;

@Repository
@RequiredArgsConstructor
public class CredentialRepositoryImpl implements CredentialRepository {

  private final JpaCredentialRepository jpaCredentialRepository;

  @Override
  public Optional<Credentials> findUserCredentialByEmail(String email) {
    return jpaCredentialRepository.findByEmail(email);
  }

  @Override
  public boolean existsByEmail(String email) {
    return jpaCredentialRepository.existsByEmail(email);
  }
}
