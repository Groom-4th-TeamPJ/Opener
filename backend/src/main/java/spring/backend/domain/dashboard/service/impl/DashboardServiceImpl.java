package spring.backend.domain.dashboard.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.dashboard.dto.response.DashboardSummaryResponse;
import spring.backend.domain.dashboard.service.spec.DashboardService;
import spring.backend.domain.exam.repository.dto.DashboardStatsRow;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final ExamResultRepository examResultRepository;

    public DashboardServiceImpl(ExamResultRepository examResultRepository) {
        this.examResultRepository = examResultRepository;
    }

    @Override
    public DashboardSummaryResponse getCorrectRate(UUID userId) {
        DashboardSummaryResponse response = getDashboardSummary(userId);

        return DashboardSummaryResponse.builder()
                .monthlyAverageCorrectRate(response.getMonthlyAverageCorrectRate())
                .monthlyQuestionsSolvedCount(response.getMonthlyQuestionsSolvedCount())
                .build();
    }

    @Override
    public DashboardSummaryResponse getTotalQuestionsSolved(UUID userId) {
        DashboardSummaryResponse response = getDashboardSummary(userId);

        return DashboardSummaryResponse.builder()
                .totalQuestionsSolvedCount(response.getTotalQuestionsSolvedCount())
                .build();
    }

    @Override
    public DashboardSummaryResponse getTotalLearningTime(UUID userId) {
        DashboardSummaryResponse response = getDashboardSummary(userId);

        return DashboardSummaryResponse.builder()
                .totalLearningTimeDesc(response.getTotalLearningTimeDesc())
                .build();
    }

    @Override
    public DashboardSummaryResponse getDashboardSummary(UUID userId) {
        DashboardStatsRow statsRow = examResultRepository.getDashboardSummary(userId);

        int monthlyAverageCorrectRate = calculateMonthlyAverageCorrectRate(
                statsRow.monthCorrectCount(),
                statsRow.monthQuestionsSolvedCount()
        );

        return DashboardSummaryResponse.builder()
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

        return (int) Math.round(((double) correctCnt / totalCnt) * 100);
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
