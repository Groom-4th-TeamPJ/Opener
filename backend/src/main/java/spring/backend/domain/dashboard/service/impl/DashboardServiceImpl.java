package spring.backend.domain.dashboard.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.dashboard.dto.response.DashboardSummaryResponse;
import spring.backend.domain.dashboard.service.spec.DashboardService;
import spring.backend.domain.exam.repository.dto.DashboardStatsRow;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.shared.infrastructure.security.dto.AuthUser;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ExamResultRepository examResultRepository;

    public DashboardServiceImpl(ExamResultRepository examResultRepository) {
        this.examResultRepository = examResultRepository;
    }

    @Override
    public DashboardSummaryResponse getCorrectRate(AuthUser authUser) {
        DashboardSummaryResponse response = getDashboardSummary(authUser);

        return DashboardSummaryResponse.builder()
                .monthlyAverageCorrectRate(response.getMonthlyAverageCorrectRate())
                .monthlyQuestionsSolvedCount(response.getMonthlyQuestionsSolvedCount())
                .build();
    }

    @Override
    public DashboardSummaryResponse getTotalQuestionsSolved(AuthUser authUser) {
        DashboardSummaryResponse response = getDashboardSummary(authUser);

        return DashboardSummaryResponse.builder()
                .totalQuestionsSolvedCount(response.getTotalQuestionsSolvedCount())
                .build();
    }

    @Override
    public DashboardSummaryResponse getTotalLearningTime(AuthUser authUser) {
        DashboardSummaryResponse response = getDashboardSummary(authUser);

        return DashboardSummaryResponse.builder()
                .totalLearningTimeDesc(response.getTotalLearningTimeDesc())
                .build();
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary(AuthUser authUser) {
        DashboardStatsRow statsRow = examResultRepository.getDashboardSummary(authUser.id());

        if (statsRow == null) {
            return DashboardSummaryResponse.builder()
                    .userName(authUser.name())
                    .monthlyAverageCorrectRate(0)
                    .monthlyQuestionsSolvedCount(0L)
                    .totalQuestionsSolvedCount(0L)
                    .totalLearningTimeDesc("0분")
                    .build();
        }

        int monthlyAverageCorrectRate = calculateMonthlyAverageCorrectRate(
                statsRow.monthCorrectCount(),
                statsRow.monthQuestionsSolvedCount()
        );

        return DashboardSummaryResponse.builder()
                .userName(authUser.name())
                .monthlyAverageCorrectRate(monthlyAverageCorrectRate)
                .monthlyQuestionsSolvedCount(statsRow.monthQuestionsSolvedCount())
                .totalQuestionsSolvedCount(statsRow.totalQuestionsSolvedCount())
                .totalLearningTimeDesc(makeTotalLearningTimeFormat(statsRow.totalTimeSpentSeconds()))
                .build();
    }

    private int calculateMonthlyAverageCorrectRate(long correctCnt, long totalCnt) {
        if (totalCnt == 0) {
            return 0;
        }

        return (int) Math.ceil(((double) correctCnt / totalCnt) * 100);
    }

    private String makeTotalLearningTimeFormat(long totalLearningTime) {
        Long days = totalLearningTime / 86400;
        Long hours = (totalLearningTime / 3600) % 24;
        Long minutes = (totalLearningTime / 60) % 60;

        StringBuilder formattedTime = new StringBuilder();
        if (days > 0) {
            formattedTime.append(days).append("일 ");
        }
        if (hours > 0) {
            formattedTime.append(hours).append("시간 ");
        }
        if (minutes > 0) {
            formattedTime.append(minutes).append("분");
        }

        return formattedTime.toString().trim().isEmpty() ? "0분" : formattedTime.toString().trim();
    }
}
