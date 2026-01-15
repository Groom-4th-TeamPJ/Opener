package spring.backend.domain.can.repository.spec;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import spring.backend.domain.can.model.entity.Can;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CanRepository {
    Optional<Can> findByUserId(UUID userId);

    Can save(Can can);

    Page<Can> findAll(Pageable pageable);

    void saveAll(List<Can> cans);
}
