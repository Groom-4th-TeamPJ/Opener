package spring.backend.domain.user.repository.jpa;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.user.model.entity.User;

public interface JpaUserRepository extends JpaRepository<User, UUID> {
}
