package spring.backend.domain.can.repository.spec;

import spring.backend.domain.can.model.entity.Can;

import java.util.Optional;
import java.util.UUID;

public interface CanRepository {
    Optional<Can> findByUserId(UUID userId);

    Can save(Can can);
}
