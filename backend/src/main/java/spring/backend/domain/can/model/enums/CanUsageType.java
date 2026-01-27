package spring.backend.domain.can.model.enums;

import lombok.Getter;

@Getter
public enum CanUsageType {

    OPENER("오프너"), SYSTEM_CHARGE("시스템 충전"), SYSTEM_RECOVERY("시스템 복구"), QUESTION_NEW("문제 생성");

    private final String name;

    CanUsageType(String name) {
        this.name = name;
    }
}
