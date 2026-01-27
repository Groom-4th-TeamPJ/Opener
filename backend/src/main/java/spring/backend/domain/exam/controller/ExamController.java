package spring.backend.domain.exam.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.backend.domain.exam.dto.response.ExamResponse;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.ExamType;
import spring.backend.domain.exam.service.spec.ExamService;
import spring.backend.shared.infrastructure.security.dto.AuthUser;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
@Tag(name = "\uD83D\uDCD8 Exam", description = "시험 문제")
public class ExamController {

    private final ExamService examService;

    @Operation (
            summary = "시험 문제 조회",
            description = "특정 연도, 유형, 카테고리에 해당하는 시험 문제를 조회합니다."
    )
    @GetMapping
    public ResponseEntity<ExamResponse> getExamWithQuestions(
            @Parameter(description = "조회할 연도", required = true) @RequestParam Integer examYear,
            @Parameter(description = "시험 유형 (예: CSAT)", required = true) @RequestParam(required = true) ExamType examType,
            @Parameter(description = "문제 카테고리 (예: ALG)", required = true) @RequestParam(required = true) Category category,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null || authUser.id() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        ExamResponse resp = examService.findExamWithQuestions(authUser.id(), examYear, examType, category);
        return ResponseEntity.ok(resp);
    }

}
