package spring.backend.shared.infrastructure.security.dto;

import java.util.UUID;

public record AuthUser(
        UUID id,
        String name,
        String jti
) {
}
