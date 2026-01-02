package spring.backend.domain.user.repository.spec;

import java.util.UUID;
import spring.backend.domain.user.model.entity.User;

public interface UserRepository {
  User findUserById(UUID id);
}
