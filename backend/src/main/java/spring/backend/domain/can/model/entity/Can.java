package spring.backend.domain.can.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.backend.shared.entity.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "user_cans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Can extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "current_cans")
    private int currentCans;

    @Column(name = "max_cans")
    private int maxCans;

    public static Can of(UUID userId, int initialCans) {
        Can can = new Can();
        can.userId = userId;
        can.currentCans = initialCans;
        can.maxCans = initialCans;
        return can;
    }
}
