package spring.backend.domain.can.service.impl;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import spring.backend.domain.can.dto.response.CanResponse;
import spring.backend.domain.can.model.entity.Can;
import spring.backend.domain.can.repository.spec.CanRepository;
import spring.backend.domain.can.service.spec.CanService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import java.beans.Transient;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class CanServiceImpl implements CanService {

    private final CanRepository canRepository;

    public CanServiceImpl(CanRepository canRepository) {
        this.canRepository = canRepository;
    }

    @Transactional
    @Override
    public CanResponse getCurrentCan(UUID userId) {
        return canRepository.findByUserId(userId)
                .map(can -> CanResponse.builder()
                        .currentCan(can.getCurrentCans())
                        .build())
                .orElseGet(() -> createUserCan(userId));
    }

    @Transactional
    @Override
    public CanResponse createUserCan(UUID userId) {
        Can saved = canRepository.save(Can.of(userId, 10));
        return CanResponse.builder()
                .currentCan(saved.getCurrentCans())
                .build();
    }

    @Transactional
    @Override
    public CanResponse updateUserCan(UUID userId, int cansToAdd) {
        return modifyAndSaveCan(userId, can -> can.addCans(cansToAdd));
    }

    @Transactional
    @Override
    public CanResponse useUserCan(UUID userId, int cansToUse) {
        return modifyAndSaveCan(userId, can -> can.useCans(cansToUse));
    }

    @Transactional
    @Override
    public CanResponse recoverUserCan(UUID userId, int cansToRecover) {
        return modifyAndSaveCan(userId, can -> can.recoverCans(cansToRecover));
    }

    private CanResponse modifyAndSaveCan(UUID userId, Consumer<Can> modifier) {
        Can can = canRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAN_NOT_FOUND));

        modifier.accept(can);
        Can updatedCan = canRepository.save(can);

        return CanResponse.builder()
                .currentCan(updatedCan.getCurrentCans())
                .build();
    }


}
