package spring.backend.domain.can.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import spring.backend.domain.exam.model.enums.ExamType;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum CanLogStatus {
    USE("사용"), RECOVER("시스템 복구"), CHARGE("충전");


    private final String name;
    CanLogStatus(String name) {
        this.name = name;
    }

    // JSON 역직렬화 ({"code":"CSAT","name":"수학능력시험"} 등 처리)
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public static CanLogStatus fromJson(@JsonProperty("code") String code,
                                    @JsonProperty("name") String name) {
        if (code != null) {
            try {
                return CanLogStatus.valueOf(code);
            } catch (IllegalArgumentException ignored) { }
        }
        if (name != null) {
            for (CanLogStatus e : values()) {
                if (e.getName().equals(name)) return e;
            }
        }
        throw new IllegalArgumentException("Unknown CanLogStatus: code=" + code + ", name=" + name);
    }
}
