package spring.backend.domain.dashboard.service.spec;

import spring.backend.domain.dashboard.dto.response.DashboardSummaryResponse;

import java.util.UUID;

public interface DashboardService {

    DashboardSummaryResponse getCorrectRate(UUID userId);

    DashboardSummaryResponse getTotalQuestionsSolved(UUID userId);

    DashboardSummaryResponse getTotalLearningTime(UUID userID);

    DashboardSummaryResponse getDashboardSummary(UUID userID);

}
