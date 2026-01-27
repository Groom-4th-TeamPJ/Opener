package spring.backend.domain.can.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import spring.backend.domain.can.model.entity.Can;
import spring.backend.domain.can.service.spec.CanService;
import spring.backend.domain.can.service.spec.CanUsageLogService;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyCanIssueScheduler {

    private final CanService canService;
    private static final int PAGE_SIZE = 100;

    // 자정에 캔 10개 충전
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
    public void runDailyIssue() {
        LocalDateTime now = LocalDateTime.now();
        log.info("일일 캔 지급 작업 시작. 시간: {}", now);
        int page = 0;
        Page<Can> result;

        do {
            int currentPage = page++;
            Pageable pageable = PageRequest.of(currentPage, PAGE_SIZE);
            try {
                result = canService.processDailyCanIssues(pageable);
            } catch (Exception e) {
                log.error("일일 캔 지급 작업 중 오류 발생. 중단 - page: {}, error: {}", currentPage, e.getMessage());
                break;
            }
        } while (result != null && result.hasNext());

        log.info("일일 캔 지급 작업 완료. 종료 시간: {}, 소요 시간: {}", LocalDateTime.now(), now.until(LocalDateTime.now(), java.time.temporal.ChronoUnit.SECONDS));
    }

}
