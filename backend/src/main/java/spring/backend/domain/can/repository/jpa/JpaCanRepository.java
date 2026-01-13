package spring.backend.domain.can.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.can.model.entity.Can;

import java.util.Optional;
import java.util.UUID;

public interface JpaCanRepository extends JpaRepository<Can, Long> {
    Optional<Can> findByUserId(UUID userId);
}
