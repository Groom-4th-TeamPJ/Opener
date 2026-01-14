package spring.backend.domain.can.repository.spec;

import spring.backend.domain.can.model.entity.CanUsageLog;

public interface CanUsageLogRepository {
    CanUsageLog save(CanUsageLog canUsageLog);
}
