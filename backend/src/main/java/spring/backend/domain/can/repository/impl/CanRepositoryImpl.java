package spring.backend.domain.can.repository.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import spring.backend.domain.can.model.entity.Can;
import spring.backend.domain.can.repository.jpa.JpaCanRepository;
import spring.backend.domain.can.repository.spec.CanRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CanRepositoryImpl implements CanRepository {

    private final JpaCanRepository jpaConRepository;

    public CanRepositoryImpl(JpaCanRepository jpaConRepository) {
        this.jpaConRepository = jpaConRepository;
    }

    @Override
    public Optional<Can> findByUserId(UUID userId) {
        return jpaConRepository.findByUserId(userId);
    }

    @Override
    public Can save(Can can) {
        return jpaConRepository.save(can);
    }

    @Override
    public Page<Can> findAll(Pageable pageable) {
        return jpaConRepository.findAll(pageable);
    }

    @Override
    public void saveAll(List<Can> cans) {
        jpaConRepository.saveAll(cans);
    }
}
