package spring.backend.shared.config;


import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    Info info = new Info()
            .title("Opener API")
            .version("v1.0.0")
            .description("""
            🔓 **OPENER**
            
            수능 문제 풀이 결과를 분석하고  
            AI 기반 피드백을 제공하는 학습 플랫폼 API
            
            ---
            ### 주요 기능
            - 🧠 문제 풀이 결과 분석
            - 💬 AI 해설 & 피드백
            """);

    @Bean
    public OpenAPI openAPI() {
        String cookieAuthName = "cookieAuth";

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes(cookieAuthName,
                                new SecurityScheme()
                                    .type(SecurityScheme.Type.APIKEY)
                                    .in(SecurityScheme.In.COOKIE)
                                    .name("accessToken")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(cookieAuthName))
                .addServersItem(new Server().url("http://localhost:8080/api").description("Local Server"))
                .addServersItem(new Server().url("https://opener.ai.kr/api").description("Production Server"))
                .info(info);
    }



    @Bean
    public OpenApiCustomizer manualLoginEndpoint() {
        return (OpenAPI openApi) -> {
            Paths paths = openApi.getPaths();
            if (paths == null) {
                paths = new Paths();
                openApi.setPaths(paths);
            }

            // 1) Form Login
            addFormLogin(paths);

            // 2) OAuth Login (예시: code 교환형)
            // 👉 여기 URL은 네 실제 엔드포인트로 바꿔!
            addOAuthLogin(paths);


        };

    }

    private void addFormLogin(Paths paths) {
        paths.addPathItem("/auth/form-login",
                new PathItem().post(
                        new Operation()
                                .tags(List.of("👤 Auth"))
                                .summary("폼 로그인")
                                .description("""
                        필터에서 처리되는 로그인 엔드포인트입니다.

                        ✅ 성공 시
                        - `accessToken`, `refreshToken` 이 **HttpOnly 쿠키**로 설정됩니다.
                        - 토큰은 **응답 바디가 아니라 `Set-Cookie` 헤더**로 내려옵니다.

                        ⚠️ Swagger UI에서 쿠키 동작은 브라우저 정책(SameSite/Secure/도메인)에 따라
                        기대대로 안 보일 수 있습니다.
                        """)
                                .requestBody(new RequestBody()
                                        .required(true)
                                        .content(new Content().addMediaType("application/json",
                                                new MediaType().schema(loginRequestSchema())
                                        ))
                                )
                                .responses(new ApiResponses()
                                        .addApiResponse("200", new ApiResponse()
                                                .description("로그인 성공 (HttpOnly 쿠키 설정)")
                                                // 바디가 없다면 content 생략해도 됨. 메시지 응답이면 아래 schema로 변경 가능.
                                                .addHeaderObject("Set-Cookie", new Header()
                                                        .description("""
                                    예: 
                                    accessToken=...; HttpOnly; Path=/; SameSite=Lax
                                    refreshToken=...; HttpOnly; Path=/; SameSite=Lax
                                    """)
                                                        .schema(new StringSchema())
                                                )
                                        )
                                        .addApiResponse("400", new ApiResponse()
                                                .description("요청 형식 오류 (email/password 누락 등)")
                                        )
                                        .addApiResponse("401", new ApiResponse()
                                                .description("로그인 실패 (자격 증명 불일치)")
                                        )
                                )
                )
        );
    }

    private void addOAuthLogin(Paths paths) {
        // ✅ 너희가 실제로 쓰는 path로 변경:
        // 예: "/api/auth/oauth-login" 또는 "/api/auth/oauth-signup"
        String path = "/auth/oauth-login";

        paths.addPathItem(path,
                new PathItem().post(
                        new Operation()
                                .tags(List.of("👤 Auth"))
                                .summary("OAuth 로그인")
                                .description("""
                    OAuth 인증 후 발급된 **authorization code**(또는 provider token)를 서버로 전달합니다.

                    ✅ 성공 시
                    - `accessToken`, `refreshToken` 이 **HttpOnly 쿠키**로 설정됩니다.
                    - 토큰은 응답 바디가 아니라 `Set-Cookie` 헤더로 내려옵니다.
                    """)
                                .requestBody(new RequestBody()
                                        .required(true)
                                        .content(new Content().addMediaType("application/json",
                                                new MediaType().schema(oauthLoginRequestSchema())
                                        ))
                                )
                                .responses(new ApiResponses()
                                        .addApiResponse("200", new ApiResponse()
                                                .description("OAuth 로그인 성공 (HttpOnly 쿠키 설정)")
                                                .addHeaderObject("Set-Cookie", cookieHeaderDoc())
                                        )
                                        .addApiResponse("400", new ApiResponse().description("요청 형식 오류"))
                                        .addApiResponse("401", new ApiResponse().description("OAuth 인증 실패"))
                                )
                )
        );
    }

    private Header cookieHeaderDoc() {
        return new Header()
                .description("""
            예:
            accessToken=...; HttpOnly; Path=/; SameSite=Lax
            refreshToken=...; HttpOnly; Path=/; SameSite=Lax
            """)
                .schema(new StringSchema());
    }

    private Schema<?> loginRequestSchema() {
        return new ObjectSchema()
                .addProperty("email", new StringSchema().example("user@test.com"))
                // 비밀번호는 보통 example만 넣고 format은 password로 두면 Swagger UI가 가려줌
                .addProperty("password", new StringSchema().format("password").example("asd123!@Q"))
                .required(List.of("email", "password"));
    }

    private Schema<?> oauthLoginRequestSchema() {
        // 가장 흔한 형태(authorization code)
        // 실제로는 provider(google/kakao/naver) + code + redirectUri 정도로 구성됨
        return new ObjectSchema()
                .addProperty("provider", new StringSchema()
                        .description("OAuth Provider")
                        .example("google"))
                .addProperty("code", new StringSchema()
                        .description("Authorization Code")
                        .example("4/0AfJohX..."))
                .addProperty("redirectUri", new StringSchema()
                        .description("OAuth Redirect URI")
                        .example("http://localhost:3000/oauth/callback"))
                .required(List.of("provider", "code", "redirectUri"));
    }
}
