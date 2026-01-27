package spring.backend.domain.exam.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import spring.backend.domain.exam.dto.request.SubmitAnswerRequest;
import spring.backend.domain.exam.dto.response.ExamResultSummaryResponse;
import spring.backend.domain.exam.dto.response.SubmitAnswerResponse;
import spring.backend.domain.exam.service.spec.ExamResultService;
import spring.backend.shared.infrastructure.security.dto.AuthUser;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
@Tag(name = "\uD83D\uDCDD ExamResult", description = "시험 결과")
public class ExamResultController {

    private final ExamResultService examResultService;

    @Operation(
            summary = "문제 정답 제출",
            description = """
            시험 진행 중 특정 문제에 대한 정답을 제출합니다.
            
            - 문제당 **최초 제출만 허용**됩니다.
            - 이미 제출된 문제는 다시 제출할 수 없습니다.
            - 제출 시 자동으로 정답 여부를 판별하고 점수/소요시간이 반영됩니다.
            """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "정답 제출 성공",
                    content = @Content(schema = @Schema(implementation = SubmitAnswerResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (중복 제출, 파라미터 오류 등)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "시험 결과 또는 문제를 찾을 수 없음"
            )
    })
    @PostMapping("/results/{examResultId}/questions/{questionId}")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
            @Parameter(description = "시험 결과 ID", example = "1")
            @PathVariable Long examResultId,
            @Parameter(description = "문제 ID", example = "10")
            @PathVariable Long questionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "정답 제출 정보",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SubmitAnswerRequest.class))
            )
            @Valid @RequestBody SubmitAnswerRequest submitAnswerRequest,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null || authUser.id() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        SubmitAnswerResponse resp = examResultService.submitAnswers(examResultId, questionId, authUser.id(), submitAnswerRequest);

        return ResponseEntity.ok(resp);
    }

    @Operation(
            summary = "시험 결과 요약 정보 조회"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "정답 제출 성공",
                    content = @Content(schema = @Schema(implementation = ExamResultSummaryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (중복 제출, 파라미터 오류 등)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "시험 결과 또는 문제를 찾을 수 없음"
            )
    })
    @GetMapping("/results/{examResultId}/summary")
    public ExamResultSummaryResponse getExamResultSummary(
            @Parameter(description = "시험 결과 ID", example = "1")
            @PathVariable Long examResultId,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null || authUser.id() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return examResultService.getExamResultSummary(examResultId, authUser.id());
    }
}
