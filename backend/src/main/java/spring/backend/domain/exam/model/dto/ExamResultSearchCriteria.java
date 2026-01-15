package spring.backend.domain.exam.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.backend.domain.exam.model.enums.ExamType;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResultSearchCriteria {

    private UUID userId;
    private Integer examYear;
    private ExamType examType;
    private LocalDateTime createdAtFrom;
    private LocalDateTime createdAtTo;
    private Boolean openerUsage;
}
