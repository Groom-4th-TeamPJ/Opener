package spring.backend.domain.auth.dto.request;

public record FormLoginRequest(
        String email,
        String password
) {
}
