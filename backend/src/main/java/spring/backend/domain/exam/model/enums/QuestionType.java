package spring.backend.domain.exam.model.enums;

public enum QuestionType {
    MCQ("객관식"), FRQ("주관식");


    private final String name;

    QuestionType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
