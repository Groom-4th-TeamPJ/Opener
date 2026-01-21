package spring.backend.domain.exam.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PassageType {
    TEXT("TEXT"),
    IMAGE("IMAGE");

    private final String value;

    PassageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PassageType fromValue(String value) {
        for (PassageType type : PassageType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown PassageType: " + value);
    }
}
