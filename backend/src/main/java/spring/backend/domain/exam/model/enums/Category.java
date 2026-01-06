package spring.backend.domain.exam.model.enums;

public enum Category {
    ALG("수학I + 수학II"),
    GEO("기하"),
    PROB("확률과 통계"),
    CALC("미적분");

    private String name;

    Category(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
