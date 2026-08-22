package br.com.fiap.infrastructure.configuration;

import br.com.fiap.domain.ports.out.VideoDownloadPort;
import br.com.fiap.domain.ports.out.VideoZipStoragePort;
import br.com.fiap.infrastructure.adapters.storage.LocalFileDownloadAdapter;
import br.com.fiap.infrastructure.adapters.storage.LocalFileZipStorageAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VideoStorageBeanConfiguration {

    @Bean
    public VideoDownloadPort localFileDownloadPort(
            @Value("${app.storage.local.path:/app/videos/uploads}") String basePath) {
        return new LocalFileDownloadAdapter(basePath);
    }

    @Bean
    public VideoZipStoragePort localFileZipStoragePort(
            @Value("${app.storage.local.output-path:/app/videos/processed}") String outputPath) {
        return new LocalFileZipStorageAdapter(outputPath);
    }
}

