package spring.backend.shared.infrastructure.security.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

/**
 * OAuth2 인증 실패 핸들러
 * 인증 실패 시 프론트엔드 에러 페이지로 리다이렉트
 */
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.failure-redirect-uri}")
    private String failureRedirectUri;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        log.error("OAuth2 인증 실패: {}", exception.getMessage(), exception);

        String errorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        String redirectUrl = failureRedirectUri + "?error=" + errorMessage;

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
