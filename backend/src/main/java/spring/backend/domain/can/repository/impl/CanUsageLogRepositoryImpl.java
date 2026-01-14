package spring.backend.domain.can.repository.impl;

import jdk.jfr.Registered;
import org.springframework.stereotype.Repository;
import spring.backend.domain.can.model.entity.CanUsageLog;
import spring.backend.domain.can.repository.jpa.JpaCanUsageLogRepository;
import spring.backend.domain.can.repository.spec.CanUsageLogRepository;

@Repository
public class CanUsageLogRepositoryImpl implements CanUsageLogRepository {

    private final JpaCanUsageLogRepository jpaCanUsageLogRepository;

    public CanUsageLogRepositoryImpl(JpaCanUsageLogRepository jpaCanUsageLogRepository) {
        this.jpaCanUsageLogRepository = jpaCanUsageLogRepository;
    }

    @Override
    public CanUsageLog save(CanUsageLog canUsageLog) {
        return jpaCanUsageLogRepository.save(canUsageLog);
    }
}
