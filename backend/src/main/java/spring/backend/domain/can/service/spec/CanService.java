package spring.backend.domain.can.service.spec;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import spring.backend.domain.can.dto.response.CanResponse;
import spring.backend.domain.can.model.entity.Can;

import java.util.UUID;

public interface CanService {

    CanResponse getCurrentCan(UUID userId);

    CanResponse createUserCan(UUID userId);

    CanResponse addUserCan(UUID userId, int cansToAdd);

    CanResponse useUserCan(UUID userId, int cansToUse);

    CanResponse recoverUserCan(UUID userId, int cansToRecover);

    Page<Can> processDailyCanIssues(Pageable pageable);

}
