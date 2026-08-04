package br.com.fiap.infrastructure.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url("/").description("Local — http://localhost:8084"))
                .info(new Info()
                        .title("FIAP X - Video Processing MS")
                        .version("1.0.0")
                        .description("""
                                FIAP - 14 SOAT - Arquitetura de Software (Turma Outubro de 2025)
                                Tech Challenge - Fase 5 (Hackathon)

                                Worker assíncrono — consome SQS video-uploaded, processa com ffmpeg,
                                publica resultado em video-events.
                                """));
    }
}
