package spring.backend.domain.user.repository.spec;

import java.util.Optional;
import spring.backend.domain.user.model.entity.Credentials;

public interface UserCredentialRepository {

  Optional<Credentials> findUserCredentialByEmail(String email);

}
