package spring.backend.shared.infrastructure.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import spring.backend.domain.auth.model.entity.Credentials;
import spring.backend.domain.auth.respository.spec.CredentialRepository;
import spring.backend.domain.user.model.entity.User;
import spring.backend.shared.response.codes.SuccessCode;
import spring.backend.shared.response.format.ApiResponseFormat;
import spring.backend.shared.infrastructure.security.service.TokenIssuer;


@Component
public class FormAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  // 발급의 유일한 진입점 -> 경로마다 복제하면 저장 누락이 조용히 생긴다
  private final TokenIssuer tokenIssuer;

  private final CredentialRepository credentialRepository;
  private final ObjectMapper objectMapper;

  public FormAuthenticationSuccessHandler(
          TokenIssuer tokenIssuer,
          CredentialRepository credentialRepository,
          ObjectMapper objectMapper
  ) {
    this.tokenIssuer = tokenIssuer;
    this.credentialRepository = credentialRepository;
    this.objectMapper = objectMapper;
  }

  // @Transactional -> 로그인 성공 부수효과(lastLoginAt 갱신·실패 카운트 리셋)를 한 단위로 커밋
  @Override
  @Transactional
  public void onAuthenticationSuccess(
          HttpServletRequest request,
          HttpServletResponse response,
          Authentication authentication
  ) throws IOException {

    // 인증된 사용자 정보에서 email 추출
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String email = userDetails.getUsername();

    // email로 Credentials 및 User 정보 조회
    Credentials credentials = credentialRepository
            .findUserCredentialByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

    // save() 호출 없음 -> 트랜잭션 안 영속 엔티티는 더티체킹으로 자동 UPDATE, 명시 저장 불필요
    credentials.recordLoginSuccess();

    // 조회한 credentials로 user 조회
    User user = credentials.getUser();

    // 발급·쿠키·Redis 저장을 한 진입점으로 -> 경로마다 복제되면 한 곳만 저장을 빠뜨려도 조용히 다르다
    tokenIssuer.issue(response, user.getId(), user.getRole(), user.getName());

    // 공통 응답 포맷으로 래핑 (data는 null)
    ApiResponseFormat<Void> apiResponse = ApiResponseFormat.success(
            SuccessCode.OK.getCode(),
            SuccessCode.OK.getMessage()
    );

    // JSON 응답 반환
    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getWriter(), apiResponse);

  }
}
