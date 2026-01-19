package spring.backend.domain.exam.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public class ExamController {

    private final ExamService examService;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.rabbitmq.username}")
    private String rabbitUserName;

    @GetMapping
    public ResponseEntity<ExamResponse> getExamWithQuestions(
            @Parameter(description = "조회할 연도", required = true) @RequestParam Integer examYear,
            @Parameter(description = "시험 유형 (예: CSAT)", required = true) @RequestParam(required = true) ExamType examType,
            @Parameter(description = "문제 카테고리 (예: ALG)", required = true) @RequestParam(required = true) Category category,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        log.info("-====================테스트================================");
        log.info("env 파일 잘 가져오는지 확인 하는 테스트 입니다. api: {}", apiKey);
        log.info("env 파일 잘 가져오는지 확인 하는 테스트 입니다. name : {}", rabbitUserName);
        log.info("-====================================================");
        if (authUser == null || authUser.id() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        ExamResponse resp = examService.findExamWithQuestions(authUser.id(), examYear, examType, category);
        return ResponseEntity.ok(resp);
    }

}
