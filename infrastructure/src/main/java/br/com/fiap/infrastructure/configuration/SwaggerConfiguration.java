package br.com.fiap.infrastructure.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class SwaggerConfiguration {

    @Value("${auth.service.url:}")
    private String authServiceUrl = "";

    @Bean
    public OpenAPI customOpenAPI() {
        java.util.List<Server> servers;
        if (StringUtils.hasText(authServiceUrl)) {
            servers = java.util.List.of(
                    new Server().url(authServiceUrl + "/video-processing").description("Via gateway (auth service)"),
                    new Server().url("/").description("Direct - http://localhost:8086")
            );
        } else {
            servers = java.util.List.of(
                    new Server().url("/").description("Local - http://localhost:8086")
            );
        }
        return new OpenAPI()
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT obtained via POST /auth/login. Use: Bearer <token>")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .info(new Info()
                        .title("FIAP X - Video Processing MS")
                        .version("1.0.0")
                        .description("""
                                FIAP - 14 SOAT - Arquitetura de Software (Turma Outubro de 2025)
                                Tech Challenge - Fase 5 (Hackathon)

                                Async worker - consumes RabbitMQ video-uploaded, processes with ffmpeg,
                                and publishes the result to video-events.

                                Authentication:
                                1. Expand the Authentication section below
                                2. Execute POST /auth/login with your credentials
                                3. Copy the token from the response
                                4. Click Authorize and enter: Bearer <token>
                                """));
    }

    @Bean
    public OpenApiCustomizer authLoginServerOverride() {
        return openApi -> {
            if (!StringUtils.hasText(authServiceUrl)) {
                return;
            }
            if (openApi.getPaths() == null) {
                return;
            }
            var authPath = openApi.getPaths().get("/auth/login");
            if (authPath != null) {
                authPath.servers(java.util.List.of(
                        new Server().url(authServiceUrl).description("Auth Service")
                ));
            }
        };
    }
}
