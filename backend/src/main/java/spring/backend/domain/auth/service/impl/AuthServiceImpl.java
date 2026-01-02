package spring.backend.domain.auth.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import spring.backend.domain.auth.dto.request.FormSignupRequest;
import spring.backend.domain.auth.dto.request.OAuthSignupRequest;
import spring.backend.domain.auth.dto.response.AuthTokens;
import spring.backend.domain.auth.model.entity.Credentials;
import spring.backend.domain.auth.respository.jpa.JpaCredentialRepository;
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
  private final JpaCredentialRepository jpaCredentialRepository;


  @Override
  public AuthTokens formSignup(FormSignupRequest req) {

    // 이메일 중복 확인
    if (credentialRepository.existsByEmail(req.email())) {
      throw new IllegalArgumentException("이미존재하는 계정"); // 이후 공통 응답으로 수정
    }

    // User 생성
    User user = User.createUser(req.name());

    // Credential 생성
    String encodedPassword = passwordEncoder.encode(req.password());

    Credentials credential = Credentials.createFormCredentials(
            user,
            req.email(),
            encodedPassword
    );

    // 저장
    Credentials newCredential = jpaCredentialRepository.save(credential);

    // 토큰 생성
    String accessToken = jwtUtil.generateAccessToken(
            newCredential.getUser().getId(),
            newCredential.getUser().getRole(),
            newCredential.getUser().getName());
    String refreshToken = jwtUtil.generateRefreshToken(newCredential.getUser().getId());

    return new AuthTokens(accessToken, refreshToken);

  }

  @Override
  public AuthTokens oauthSignup(OAuthSignupRequest req) {
    return null;
  }

  @Override
  public void logout(String accessToken, String refreshToken) {

  }

  @Override
  public AuthTokens refreshToken(String refreshToken) {
    return null;
  }
}
