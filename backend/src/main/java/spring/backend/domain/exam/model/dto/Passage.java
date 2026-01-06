package spring.backend.domain.exam.model.dto;

import lombok.Getter;
import lombok.Setter;
import spring.backend.domain.exam.model.enums.PassageType;

@Getter
@Setter
public class Passage {
    Integer order;
    PassageType type;
    String content;

    public Passage() {}
}
