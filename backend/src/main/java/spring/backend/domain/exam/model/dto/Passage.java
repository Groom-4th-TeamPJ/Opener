package spring.backend.domain.exam.model.dto;

import lombok.Getter;
import lombok.Setter;
import spring.backend.domain.exam.model.enums.PassageType;

@Getter
@Setter
public class Passage {
    private Integer order;
    private PassageType type;
    private String content;

    public Passage() {}
}
