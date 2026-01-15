package spring.backend.domain.exam.model.dto;

import lombok.*;
import spring.backend.domain.exam.model.enums.ExamType;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExamResultSearchCriteria {

    private UUID userId;
    private Integer examYear;
    private ExamType examType;
    private LocalDateTime createdAtFrom;
    private LocalDateTime createdAtTo;
    private Boolean openerUsage;
}
