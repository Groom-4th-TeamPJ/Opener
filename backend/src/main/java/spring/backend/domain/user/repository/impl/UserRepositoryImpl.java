package spring.backend.domain.user.repository.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import spring.backend.domain.user.model.entity.User;
import spring.backend.domain.user.repository.jpa.JpaUserRepository;
import spring.backend.domain.user.repository.spec.UserRepository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

  private final JpaUserRepository jpaUserRepository;

  @Override
  public User findUserById(UUID id) {
    return jpaUserRepository.findById(id).orElse(null);
  }
}
