package spring.backend.domain.can.service.impl;

import org.springframework.stereotype.Service;
import spring.backend.domain.can.dto.response.CanResponse;
import spring.backend.domain.can.model.entity.Can;
import spring.backend.domain.can.repository.spec.CanRepository;
import spring.backend.domain.can.service.spec.CanService;

import java.util.UUID;

@Service
public class CanServiceImpl implements CanService {

    private final CanRepository canRepository;

    public CanServiceImpl(CanRepository canRepository) {
        this.canRepository = canRepository;
    }

    @Override
    public CanResponse getCurrentCan(UUID userId) {
        return canRepository.findByUserId(userId)
                .map(can -> CanResponse.builder()
                        .currentCan(can.getCurrentCans())
                        .build())
                .orElseGet(() -> createUserCan(userId));
    }

    @Override
    public CanResponse createUserCan(UUID userId) {
        Can saved = canRepository.save(Can.of(userId, 10));
        return CanResponse.builder()
                .currentCan(saved.getCurrentCans())
                .build();
    }
}
