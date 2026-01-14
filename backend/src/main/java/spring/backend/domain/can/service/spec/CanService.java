package spring.backend.domain.can.service.spec;

import spring.backend.domain.can.dto.response.CanResponse;

import java.util.UUID;

public interface CanService {

    CanResponse getCurrentCan(UUID userId);

    CanResponse createUserCan(UUID userId);

    CanResponse updateUserCan(UUID userId, int cansToAdd);

    CanResponse useUserCan(UUID userId, int cansToUse);

    CanResponse recoverUserCan(UUID userId, int cansToRecover);


}
