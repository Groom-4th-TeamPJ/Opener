package spring.backend.domain.can.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.backend.domain.can.model.enums.CanLogStatus;
import spring.backend.domain.can.model.enums.CanUsageType;
import spring.backend.shared.entity.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "can_usage_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CanUsageLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false)
    private CanUsageType usageType;

    @Column(name = "cans_used", nullable = false)
    private int cansUsed;

    @Column(name="related_id")
    private Long relatedId;

    @Column(name="cans_before", nullable = false)
    private int cansBefore;

    @Column(name="cans_after", nullable = false)
    private int cansAfter;

    @Enumerated(EnumType.STRING)
    @Column(name="status", nullable = false)
    private CanLogStatus status;

     public static CanUsageLog of(UUID userId, CanUsageType usageType, int cansUsed, Long relatedId,
                       int cansBefore, int cansAfter, CanLogStatus status) {
        CanUsageLog log = new CanUsageLog();
        log.userId = userId;
        log.usageType = usageType;
        log.cansUsed = cansUsed;
        log.relatedId = relatedId;
        log.cansBefore = cansBefore;
        log.cansAfter = cansAfter;
        log.status = status;
        return log;
    }
}
