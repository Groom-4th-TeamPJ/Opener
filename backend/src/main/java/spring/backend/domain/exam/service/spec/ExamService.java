package spring.backend.domain.exam.service.spec;


import spring.backend.domain.exam.dto.response.ExamResponse;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.ExamType;

import java.util.UUID;

public interface ExamService {

    ExamResponse findExamWithQuestions(UUID userId, Integer examYear, ExamType examType, Category category);
}
