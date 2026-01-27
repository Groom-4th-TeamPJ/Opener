package spring.backend.domain.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.backend.domain.dashboard.dto.response.DashboardSummaryResponse;
import spring.backend.domain.dashboard.service.spec.DashboardService;
import spring.backend.shared.infrastructure.security.dto.AuthUser;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@RequestMapping("/dashboard")
@RestController
@Tag(name = "📊 Dashboard", description = "학습 현황 대시보드")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(
            summary = "정답률 조회",
            description = "사용자의 전체 정답률을 조회합니다."
    )
    @GetMapping("/metrics/correct-rate")
    public DashboardSummaryResponse getCorrectRate(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return dashboardService.getCorrectRate(authUser);
    }

    @Operation (
            summary = "총 푼 문제 수 조회",
            description = "사용자가 푼 전체 문제 수를 조회합니다."
    )
    @GetMapping("/metrics/total-questions-solved")
    public DashboardSummaryResponse getTotalQuestionsSolved(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return dashboardService.getTotalQuestionsSolved(authUser);
    }

    @Operation (
            summary = "총 학습 시간 조회",
            description = "사용자의 총 학습 시간을 조회합니다."
    )
    @GetMapping("/metrics/total-learning-time")
    public DashboardSummaryResponse getTotalLearningTime(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return dashboardService.getTotalLearningTime(authUser);
    }


    @Operation(
            summary = "대시보드 요약 정보 조회 (정답률, 총 학습 시간, 총 푼 문제수 조회)",
            description = "사용자의 대시보드 요약 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = DashboardSummaryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없습니다."
            )
    })
    @GetMapping("/summary")
    public DashboardSummaryResponse getDashboardSummary(
            @AuthenticationPrincipal AuthUser authUser
            ) {
        if (authUser == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return dashboardService.getDashboardSummary(authUser);
    }
}
