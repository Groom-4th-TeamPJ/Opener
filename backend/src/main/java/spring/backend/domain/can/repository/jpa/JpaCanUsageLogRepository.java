package spring.backend.domain.can.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.can.model.entity.CanUsageLog;

public interface JpaCanUsageLogRepository extends JpaRepository<CanUsageLog, Long> {

}
