package spring.backend.domain.chat.model.vo;


import spring.backend.domain.chat.model.enums.PassageType;

public record Passage(Integer order, PassageType type, String content) {
}