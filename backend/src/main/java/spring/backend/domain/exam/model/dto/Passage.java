package spring.backend.domain.exam.model.dto;

import spring.backend.domain.exam.model.enums.PassageType;

public record Passage(Integer order, PassageType type, String content) {
}
