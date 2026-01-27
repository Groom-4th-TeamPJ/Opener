package spring.backend.domain.can.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.backend.domain.can.model.enums.CanLogStatus;
import spring.backend.domain.can.model.enums.CanUsageType;

import java.util.UUID;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LogCanUsageCommand {

    UUID userId;
    CanUsageType usageType;
    int cansUsed;
    Long relatedId;
    int cansBefore;
    int cansAfter;
    CanLogStatus status;
}
