package spring.backend.domain.auth.respository.spec;

import java.util.Optional;
import spring.backend.domain.auth.model.entity.Credentials;

public interface CredentialRepository {

  Optional<Credentials> findUserCredentialByEmail(String email);

  Optional<Credentials> findUserCredentialByProviderId(String providerId);

  boolean existsByEmail(String email);

  Credentials save(Credentials credentials);
}
