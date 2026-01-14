package spring.backend.domain.can.service.spec;

import spring.backend.domain.can.model.dto.LogCanUsageCommand;

public interface CanUsageLogService {

    void logCanUsage(LogCanUsageCommand command);
}
