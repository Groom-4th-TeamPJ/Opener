package spring.backend.domain.auth.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.backend.domain.auth.dto.request.FormSignupRequest;
import spring.backend.domain.auth.dto.request.OAuthSignupRequest;
import spring.backend.domain.auth.dto.response.TokenResponse;
import spring.backend.domain.auth.service.spec.AuthService;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

  @Override
  public TokenResponse formSignup(FormSignupRequest req) {
    return null;
  }

  @Override
  public TokenResponse oauthSignup(OAuthSignupRequest req) {
    return null;
  }

  @Override
  public void logout(String accessToken, String refreshToken) {

  }

  @Override
  public TokenResponse refreshToken(String refreshToken) {
    return null;
  }
}
