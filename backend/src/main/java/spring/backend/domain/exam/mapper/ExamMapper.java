package spring.backend.domain.exam.mapper;

import org.springframework.stereotype.Component;
import spring.backend.domain.exam.dto.response.ExamInfo;
import spring.backend.domain.exam.dto.response.ExamResponse;
import spring.backend.domain.exam.dto.response.QuestionResponse;
import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.entity.Question;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ExamMapper {

    public ExamResponse toDto(Exam exam, List<Question> questions) {
        if (exam == null) return null;

        ExamInfo info = ExamInfo.builder()
                .examId(exam.getId())
                .examYear(exam.getExamYear())
                .examType(exam.getExamType())
                .quantity(questions.size())
                .timeLimit(exam.getTimeLimit())
                .build();

        List<QuestionResponse> qs = (questions == null) ? List.of()
                : questions.stream().map(this::toQuestionResponse).collect(Collectors.toList());

        return ExamResponse.builder()
                .exam(info)
                .questions(qs)
                .build();
    }

    private QuestionResponse toQuestionResponse(Question q) {
        return QuestionResponse.builder()
                .questionId(q.getId())
                .questionNo(q.getQuestionNo())
                .category(q.getCategory())
                .point(q.getPoint())
                .questionType(q.getQuestionType())
                .passages(q.getPassages())
                .options(q.getOptions())
                .answer(q.getAnswer())
                .build();
    }
}
