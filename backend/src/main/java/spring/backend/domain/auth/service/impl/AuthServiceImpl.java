package spring.backend.domain.auth.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import spring.backend.domain.auth.dto.request.FormSignupRequest;
import spring.backend.domain.auth.dto.request.OAuthSignupRequest;
import spring.backend.domain.auth.dto.response.TokenResponse;
import spring.backend.domain.auth.model.entity.Credentials;
import spring.backend.domain.auth.respository.spec.CredentialRepository;
import spring.backend.domain.auth.service.spec.AuthService;
import spring.backend.domain.user.model.entity.User;
import spring.backend.shared.infrastructure.security.util.JwtUtil;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

  private final CredentialRepository credentialRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;


  @Override
  public TokenResponse formSignup(FormSignupRequest req) {

    // 이메일 중복 확인
    if (credentialRepository.existsByEmail(req.email())) {
      throw new IllegalArgumentException("이미존재하는 계정"); // 이후 공통 응답으로 수정
    }

    // User 생성
    User newUser = User.createUser(req.name());

    // Credential 생성
    String encodedPassword = passwordEncoder.encode(req.password());

    Credentials newCredential = Credentials.createFormCredentials(
            newUser,
            req.email(),
            encodedPassword
    );

    // 토큰 생성
    String accessToken = jwtUtil.generateAccessToken(newUser.getId(), newUser.getRole(), newUser.getName());
    String refreshToken = jwtUtil.generateRefreshToken(newUser.getId());

    return new TokenResponse(accessToken, refreshToken);

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
