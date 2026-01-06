package spring.backend.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // Swagger UI Authorization 버튼 생성
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))

                // JWT 인증 스키마 정의
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))

                // 문서 정보
                .info(new Info()
                        .title("Opener API")
                        .version("1.0.0")
                        .description("Opener API 문서")
                )

                // 접근 주소
                .addServersItem(new Server()
                        .url("http://localhost:8080/api")
                );
    }
}
