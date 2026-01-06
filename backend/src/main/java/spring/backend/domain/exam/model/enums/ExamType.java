package spring.backend.domain.exam.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ExamType {

    M06("6월 모의고사"),
    M09("9월 모의고사"),
    CSAT("수학능력시험");

    private final String name;

    ExamType(String name) {
        this.name = name;
    }

    public String getCode() {
        return name();
    }

    public String getName() {
        return name;
    }

    // JSON 역직렬화 ({"code":"CSAT","name":"수학능력시험"} 등 처리)
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static ExamType fromJson(@JsonProperty("code") String code,
                                    @JsonProperty("name") String name) {
        if (code != null) {
            try {
                return ExamType.valueOf(code);
            } catch (IllegalArgumentException ignored) { }
        }
        if (name != null) {
            for (ExamType e : values()) {
                if (e.getName().equals(name)) return e;
            }
        }
        throw new IllegalArgumentException("Unknown ExamType: code=" + code + ", name=" + name);
    }
}
