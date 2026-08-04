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

    @Value("${auth.lambda.url:}")
    private String authLambdaUrl = "";

    @Bean
    public OpenAPI customOpenAPI() {
        java.util.List<Server> servers;
        if (StringUtils.hasText(authLambdaUrl)) {
            servers = java.util.List.of(
                    new Server().url(authLambdaUrl + "/video-processing").description("AWS API Gateway (producao)")
            );
        } else {
            servers = java.util.List.of(
                    new Server().url("/").description("Local — http://localhost:8084")
            );
        }
        return new OpenAPI()
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT obtido via POST /auth/login. Informe: Bearer <token>")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .info(new Info()
                        .title("FIAP X - Video Processing MS")
                        .version("1.0.0")
                        .description("""
                                FIAP - 14 SOAT - Arquitetura de Software (Turma Outubro de 2025)
                                Tech Challenge - Fase 5 (Hackathon)

                                Worker assíncrono — consome SQS video-uploaded, processa com ffmpeg,
                                publica resultado em video-events.

                                **Como autenticar:**
                                1. Expanda a secao **Authentication** abaixo
                                2. Execute `POST /auth/login` com suas credenciais
                                3. Copie o `token` da resposta
                                4. Clique em **Authorize** (🔒) e informe: `Bearer <token>`
                                """));
    }

    @Bean
    public OpenApiCustomizer authLoginServerOverride() {
        return openApi -> {
            if (!StringUtils.hasText(authLambdaUrl)) return;
            if (openApi.getPaths() == null) return;
            var authPath = openApi.getPaths().get("/auth/login");
            if (authPath != null) {
                authPath.servers(java.util.List.of(
                        new Server().url(authLambdaUrl).description("Auth Lambda — API Gateway")
                ));
            }
        };
    }
}
