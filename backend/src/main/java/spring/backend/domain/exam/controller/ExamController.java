package spring.backend.domain.exam.controller;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import spring.backend.domain.exam.dto.response.ExamResponse;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.ExamType;
import spring.backend.domain.exam.service.spec.ExamService;

import java.util.Optional;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @GetMapping
    public ResponseEntity<ExamResponse> getExamWithQuestions(
            @Parameter(description = "조회할 연도", required = true) @RequestParam Integer examYear,
            @Parameter(description = "시험 유형 (예: CSAT)", required = true) @RequestParam(required = true) ExamType examType,
            @Parameter(description = "문제 카테고리 (예: ALG)", required = true) @RequestParam(required = true) Category category
    ) {
        Optional<ExamResponse> resp = examService.findExamWithQuestions(examYear, examType, category);
        return resp.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

}
