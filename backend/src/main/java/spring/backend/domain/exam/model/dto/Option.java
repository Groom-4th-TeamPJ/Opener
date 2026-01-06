package spring.backend.domain.exam.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Option {
    private Integer order;
    private String content;

    public Option() {}
}
