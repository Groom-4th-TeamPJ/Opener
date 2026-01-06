package spring.backend.domain.exam.model.enums;

public enum ExamType {

    M06("6월 모의고사"),
    M09("9월 모의고사"),
    CSAT("수학능력시험");

    private String name;

    ExamType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
