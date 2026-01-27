package spring.backend.domain.exam.repository.dto;

public record DashboardStatsRow (long totalTimeSpentSeconds, long totalQuestionsSolvedCount, long monthQuestionsSolvedCount, long monthCorrectCount) {
}
