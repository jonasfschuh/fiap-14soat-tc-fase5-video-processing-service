package br.com.fiap.infrastructure.configuration;

import br.com.fiap.domain.ports.out.VideoDownloadPort;
import br.com.fiap.domain.ports.out.VideoZipStoragePort;
import br.com.fiap.infrastructure.adapters.storage.LocalFileDownloadAdapter;
import br.com.fiap.infrastructure.adapters.storage.LocalFileZipStorageAdapter;
import br.com.fiap.infrastructure.adapters.storage.S3DownloadAdapter;
import br.com.fiap.infrastructure.adapters.storage.S3ZipStorageAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class VideoStorageBeanConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
    public VideoDownloadPort localFileDownloadPort(
            @Value("${app.storage.local.path:./storage}") String basePath) {
        return new LocalFileDownloadAdapter(basePath);
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
    public VideoDownloadPort s3DownloadPort(
            S3Client s3Client,
            @Value("${app.storage.s3.bucket:fiap-video-uploads}") String bucket) {
        return new S3DownloadAdapter(s3Client, bucket);
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
    public VideoZipStoragePort localFileZipStoragePort(
            @Value("${app.storage.local.output-path:./outputs}") String outputPath) {
        return new LocalFileZipStorageAdapter(outputPath);
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
    public VideoZipStoragePort s3ZipStoragePort(
            S3Client s3Client,
            @Value("${app.storage.s3.bucket:fiap-video-uploads}") String bucket) {
        return new S3ZipStorageAdapter(s3Client, bucket);
    }
}
