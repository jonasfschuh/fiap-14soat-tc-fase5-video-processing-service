package br.com.fiap.infrastructure.configuration;

import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.ports.in.ProcessVideoInputPort;
import br.com.fiap.domain.ports.out.FfmpegPort;
import br.com.fiap.domain.ports.out.VideoDownloadPort;
import br.com.fiap.domain.ports.out.VideoProcessingEventPublisherPort;
import br.com.fiap.domain.ports.out.VideoZipStoragePort;
import br.com.fiap.domain.usecases.ProcessVideoUseCase;
import br.com.fiap.infrastructure.adapters.ffmpeg.FfmpegAdapter;
import br.com.fiap.infrastructure.adapters.messaging.SqsVideoProcessingEventPublisherAdapter;
import br.com.fiap.infrastructure.logging.SqsMessageLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

@Configuration
public class ProcessingBeanConfiguration {

    @Value("${app.processing.ffmpeg.timeout-minutes:10}")
    private long ffmpegTimeoutMinutes;

    @Value("${aws.sqs.queues.video-events:http://localhost:4566/000000000000/video-events}")
    private String videoEventsQueueUrl;

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

    @Bean
    @ConditionalOnProperty(name = "aws.sqs.enabled", havingValue = "true", matchIfMissing = false)
    public VideoProcessingEventPublisherPort sqsVideoProcessingEventPublisherPort(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            SqsMessageLogger sqsMessageLogger) {
        return new SqsVideoProcessingEventPublisherAdapter(sqsClient, objectMapper,
                sqsMessageLogger, videoEventsQueueUrl);
    }

    @Bean
    @ConditionalOnProperty(name = "aws.sqs.enabled", havingValue = "false", matchIfMissing = true)
    public VideoProcessingEventPublisherPort noOpVideoProcessingEventPublisherPort() {
        return new VideoProcessingEventPublisherPort() {
            @Override
            public void publishProcessed(VideoJobResult result) {
            }

            @Override
            public void publishFailed(UUID videoId, String userId, String errorMessage) {
            }
        };
    }
}
