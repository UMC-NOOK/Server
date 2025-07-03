package umc.nook.common.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class SwaggerConfig implements WebMvcConfigurer {

    @Bean
    public OpenAPI api() {
        // JWT Bearer 방식의 SecurityScheme 정의
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP) // HTTP 방식
                .scheme("bearer") // 인증 방식은 Bearer
                .bearerFormat("JWT") // Bearer 포맷 명시
                .in(SecurityScheme.In.HEADER)
                .name("Authorization"); // 헤더 이름

        // 모든 요청에 Bearer Token 요구
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("BearerAuth");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("BearerAuth", securityScheme))
                .addSecurityItem(securityRequirement)
                .addServersItem(new Server().url("/").description("로컬 서버"))
                .info(new Info()
                        .title("NOOK API 명세서")
                        .description("NOOK API 명세서입니다.")
                        .version("v0.0.1"));
    }
}
