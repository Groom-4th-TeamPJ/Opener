package spring.backend.shared.infrastructure.security.util;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtUtil {
  public static final String TOKEN_PREFIX = "Bearer ";
  public static final String HEADER_STRING = "Authorization";
}
