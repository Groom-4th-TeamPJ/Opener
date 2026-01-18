package spring.backend.domain.chat.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum Category {
    ALG("수학I + 수학II"),
    GEO("기하"),
    PROB("확률과 통계"),
    CALC("미적분");

    private final String name;

    Category(String name) {
        this.name = name;
    }

    // JSON 역직렬화 ({"code":"PROB","name":"확률과 통계"} 등 처리)
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static Category fromJson(@JsonProperty("code") String code,
                                    @JsonProperty("name") String name) {
        if (code != null) {
            try {
                return Category.valueOf(code);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (name != null) {
            for (Category e : values()) {
                if (e.getName().equals(name)) {
                    return e;
                }
            }
        }
        throw new IllegalArgumentException("Unknown Category: code=" + code + ", name=" + name);
    }

    public String getCode() {
        return name();
    }

    public String getName() {
        return name;
    }
}
