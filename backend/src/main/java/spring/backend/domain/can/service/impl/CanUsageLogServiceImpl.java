package spring.backend.domain.can.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import spring.backend.domain.can.model.dto.LogCanUsageCommand;
import spring.backend.domain.can.model.entity.CanUsageLog;
import spring.backend.domain.can.repository.spec.CanUsageLogRepository;
import spring.backend.domain.can.service.spec.CanUsageLogService;

@Slf4j
@Service
public class CanUsageLogServiceImpl implements CanUsageLogService {

    private final CanUsageLogRepository canUsageLogRepository;

    public CanUsageLogServiceImpl(CanUsageLogRepository canUsageLogRepository) {
        this.canUsageLogRepository = canUsageLogRepository;
    }


    /**
     * 캔 사용 로그 기록
     * command 에 담긴 정보를 기반으로 로그를 생성하고 저장합니다.
     * command 필드:
     * - userId: 사용자 ID
     * - usageType: 캔 사용 유형 (예: OPENER, SYSTEM_CHARGE, SYSTEM_RECOVERY, QUESTION_NEW)
     * - cansUsed: 사용된 캔의 수
     * - relatedId: 관련된 엔티티 ID (예: 오프너 ID or 문제 ID)
     * - cansBefore: 사용 전 캔 수
     * - cansAfter: 사용 후 캔 수
     * - status: 로그 상태 (예: USE, RECOVER, CHARGE)
     *
     * @param command
     */
    @Override
    public void logCanUsage(LogCanUsageCommand command) {
        log.info("Can Usage Log - UserId: {}, UsageType: {}, CansUsed: {}, RelatedId: {}, CansBefore: {}, CansAfter: {}, Status: {}",
                command.getUserId(), command.getUsageType(), command.getCansUsed(), command.getRelatedId(), command.getCansBefore(), command.getCansAfter(), command.getStatus());

        CanUsageLog logEntry = CanUsageLog.of(
                command.getUserId(),
                command.getUsageType(),
                command.getCansUsed(),
                command.getRelatedId(),
                command.getCansBefore(),
                command.getCansAfter(),
                command.getStatus()
        );

        canUsageLogRepository.save(logEntry);
        log.info("캔 사용 로그가 성공적으로 저장되었습니다. 로그 ID: {}", logEntry.getId());

    }
}
