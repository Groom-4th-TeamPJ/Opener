package spring.backend.domain.user.repository.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import spring.backend.domain.user.model.entity.Credentials;
import spring.backend.domain.user.repository.jpa.JpaUserCredentialRepository;
import spring.backend.domain.user.repository.spec.UserCredentialRepository;

@Repository
@RequiredArgsConstructor
public class UserCredentialRepositoryImpl implements UserCredentialRepository {

  private final JpaUserCredentialRepository jpaUserCredentialRepository;

  @Override
  public Optional<Credentials> findUserCredentialByEmail(String email) {
    return jpaUserCredentialRepository.findByEmail(email);
  }

}
