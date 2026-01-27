package spring.backend.domain.can.service.impl;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import spring.backend.domain.can.dto.response.CanResponse;
import spring.backend.domain.can.model.dto.LogCanUsageCommand;
import spring.backend.domain.can.model.entity.Can;
import spring.backend.domain.can.model.entity.CanUsageLog;
import spring.backend.domain.can.model.enums.CanLogStatus;
import spring.backend.domain.can.model.enums.CanUsageType;
import spring.backend.domain.can.repository.spec.CanRepository;
import spring.backend.domain.can.service.spec.CanService;
import spring.backend.domain.can.service.spec.CanUsageLogService;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import java.beans.Transient;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Service
public class CanServiceImpl implements CanService {

    private final CanRepository canRepository;
    private final int DAILY_CAN_ISSUE_AMOUNT = 10;
    private final CanUsageLogService canUsageLogService;

    public CanServiceImpl(CanRepository canRepository, CanUsageLogService canUsageLogService) {
        this.canRepository = canRepository;
        this.canUsageLogService = canUsageLogService;
    }

    @Transactional
    @Override
    public CanResponse getCurrentCan(UUID userId) {
        log.info("현재 캔 조회 시도 - userId: {}", userId);

        return canRepository.findByUserId(userId)
                .map(can -> CanResponse.builder()
                        .currentCan(can.getCurrentCans())
                        .build())
                .orElseGet(() -> createUserCan(userId));
    }

    @Transactional
    @Override
    public CanResponse createUserCan(UUID userId) {
        log.info("새로운 캔 생성 시도 - userId: {}", userId);

        Can saved = canRepository.save(Can.of(userId, 10));
        return CanResponse.builder()
                .currentCan(saved.getCurrentCans())
                .build();
    }

    @Transactional
    @Override
    public CanResponse addUserCan(UUID userId, int cansToAdd) {
        log.info("캔 추가 시도 - userId: {}, cansToAdd: {}", userId, cansToAdd);

        return modifyAndSaveCan(userId, can -> can.addCans(cansToAdd));
    }

    @Transactional
    @Override
    public CanResponse useUserCan(UUID userId, int cansToUse) {
        log.info("캔 사용 시도 - userId: {}, cansToUse: {}", userId, cansToUse);

        return modifyAndSaveCan(userId, can -> can.useCans(cansToUse));
    }

    @Transactional
    @Override
    public CanResponse recoverUserCan(UUID userId, int cansToRecover) {
        log.info("캔 회복 시도 - userId: {}, cansToRecover: {}", userId, cansToRecover);
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

    // 일일 캔 지급 처리 메서드, 트랜잭션 및 재시도 적용
    // 일시적 데이터 접근 오류 발생 시 최대 3회 재시도, 재시도 간격 2초
    @Transactional
    @Retryable(retryFor = {TransientDataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    @Override
    public Page<Can> processDailyCanIssues(Pageable pageable) {
        log.info("일일 캔 지급 시작 - pageIndex={}, pageSize={}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Can> page = canRepository.findAll(pageable);
        if(page.isEmpty()) {
            return page;
        }

        page.forEach(can -> {
            can.addCans(DAILY_CAN_ISSUE_AMOUNT); // 캔 10개 추가
            logCanUsage(can); // 캔 사용 로그 기록
        });

        canRepository.saveAll(page.getContent());

        return page;
    }

    // 재시도 실패 시 호출되는 복구 메서드
    @Recover
    public Page<Can> processDailyCanPageRecover(Exception e, Pageable pageable) {
        log.error("캔 지급 재시도 실패 - pageable: {}, error: {}", pageable, e.getMessage(), e);
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    private void logCanUsage(Can can) {
        log.info("캔 사용 로그 기록 시도 - userId: {}, cansAdded: {}", can.getUserId(), DAILY_CAN_ISSUE_AMOUNT);
        int afterCans = can.getCurrentCans() + DAILY_CAN_ISSUE_AMOUNT > can.getMaxCans() ? can.getMaxCans() : can.getCurrentCans() + DAILY_CAN_ISSUE_AMOUNT;
        LogCanUsageCommand command = LogCanUsageCommand.builder()
                .userId(can.getUserId())
                .usageType(CanUsageType.SYSTEM_CHARGE)
                .cansUsed(DAILY_CAN_ISSUE_AMOUNT)
                .relatedId(can.getId())
                 .cansBefore(can.getCurrentCans())
                .cansAfter(afterCans)
                .status(CanLogStatus.CHARGE)
                .build();

        canUsageLogService.logCanUsage(command);
    }
}
