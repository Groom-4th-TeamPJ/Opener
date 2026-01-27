package spring.backend.domain.dashboard.service.spec;

import spring.backend.domain.dashboard.dto.response.DashboardSummaryResponse;
import spring.backend.shared.infrastructure.security.dto.AuthUser;

import java.util.UUID;

public interface DashboardService {

    DashboardSummaryResponse getCorrectRate(AuthUser authUser);

    DashboardSummaryResponse getTotalQuestionsSolved(AuthUser authUser);

    DashboardSummaryResponse getTotalLearningTime(AuthUser authUser);

    DashboardSummaryResponse getDashboardSummary(AuthUser authUser);

}
