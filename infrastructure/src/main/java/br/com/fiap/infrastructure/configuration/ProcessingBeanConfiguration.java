package br.com.fiap.infrastructure.configuration;

import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.ports.in.ProcessVideoInputPort;
import br.com.fiap.domain.ports.out.FfmpegPort;
import br.com.fiap.domain.ports.out.VideoDownloadPort;
import br.com.fiap.domain.ports.out.VideoProcessingEventPublisherPort;
import br.com.fiap.domain.ports.out.VideoZipStoragePort;
import br.com.fiap.domain.usecases.ProcessVideoUseCase;
import br.com.fiap.infrastructure.adapters.ffmpeg.FfmpegAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

@Configuration
public class ProcessingBeanConfiguration {

    @Value("${app.processing.ffmpeg.timeout-minutes:10}")
    private long ffmpegTimeoutMinutes;

    @Bean
    public Deque<VideoJobResult> jobHistory() {
        return new ConcurrentLinkedDeque<>();
    }

    @Bean
    public FfmpegPort ffmpegPort() {
        return new FfmpegAdapter(ffmpegTimeoutMinutes);
    }

    @Bean
    public ProcessVideoInputPort processVideoInputPort(VideoDownloadPort downloadPort,
                                                       FfmpegPort ffmpegPort,
                                                       VideoZipStoragePort zipStoragePort,
                                                       VideoProcessingEventPublisherPort eventPublisher) {
        return new ProcessVideoUseCase(downloadPort, ffmpegPort, zipStoragePort, eventPublisher);
    }
}

