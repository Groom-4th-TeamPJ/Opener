package spring.backend.domain.exam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.backend.domain.exam.model.dto.Option;
import spring.backend.domain.exam.model.dto.Passage;
import spring.backend.domain.exam.model.enums.Category;
import spring.backend.domain.exam.model.enums.QuestionType;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private Long questionId;
    private Integer questionNo;
    private Category category;
    private Integer point;
    private QuestionType questionType;
    private List<Passage> passages;
    private List<Option> options;
}
