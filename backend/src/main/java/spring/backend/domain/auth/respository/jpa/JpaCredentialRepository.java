package spring.backend.domain.auth.respository.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.auth.model.entity.Credentials;

public interface JpaCredentialRepository extends JpaRepository<Credentials, UUID> {
  Optional<Credentials> findByEmail(String email);

  boolean existsByEmail(String email);
}
