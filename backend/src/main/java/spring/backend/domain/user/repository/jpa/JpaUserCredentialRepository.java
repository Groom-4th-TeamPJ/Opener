package spring.backend.domain.user.repository.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.user.model.entity.Credentials;

public interface JpaUserCredentialRepository extends JpaRepository<Credentials, UUID> {
  Optional<Credentials> findByEmail(String email);
}
