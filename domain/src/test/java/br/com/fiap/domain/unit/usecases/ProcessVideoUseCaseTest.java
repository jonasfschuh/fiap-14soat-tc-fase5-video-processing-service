package br.com.fiap.domain.unit.usecases;

import br.com.fiap.domain.exceptions.FfmpegProcessingException;
import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.model.VideoJobStatus;
import br.com.fiap.domain.model.VideoUploadedEvent;
import br.com.fiap.domain.ports.out.FfmpegPort;
import br.com.fiap.domain.ports.out.VideoDownloadPort;
import br.com.fiap.domain.ports.out.VideoProcessingEventPublisherPort;
import br.com.fiap.domain.ports.out.VideoZipStoragePort;
import br.com.fiap.domain.usecases.ProcessVideoUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessVideoUseCaseTest {

    @Mock
    private VideoDownloadPort downloadPort;
    @Mock
    private FfmpegPort ffmpegPort;
    @Mock
    private VideoZipStoragePort zipStoragePort;
    @Mock
    private VideoProcessingEventPublisherPort eventPublisher;

    private ProcessVideoUseCase useCase;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        useCase = new ProcessVideoUseCase(downloadPort, ffmpegPort, zipStoragePort, eventPublisher);
    }

    @Test
    void shouldProcessVideoSuccessfully() throws IOException {
        UUID videoId = UUID.randomUUID();
        VideoUploadedEvent event = new VideoUploadedEvent(videoId, "user-1",
                "videos/user-1/uuid/video.mp4", "video.mp4", 1024L, "video/mp4", "2025-01-01T10:00:00Z");

        Path fakeVideo = tempDir.resolve("video.mp4");
        Files.writeString(fakeVideo, "fake");
        when(downloadPort.download(anyString())).thenReturn(fakeVideo);
        when(ffmpegPort.extractFrames(any(), any())).thenReturn(5);
        when(zipStoragePort.store(anyString(), any())).thenReturn("outputs/user-1/uuid/frames.zip");

        VideoJobResult result = useCase.execute(event);

        assertThat(result.getStatus()).isEqualTo(VideoJobStatus.DONE);
        assertThat(result.getFrameCount()).isEqualTo(5);
        verify(eventPublisher).publishProcessed(any());
        verify(eventPublisher, never()).publishFailed(any(), any(), any());
    }

    @Test
    void shouldPublishFailedEventWhenFfmpegFails() throws IOException {
        UUID videoId = UUID.randomUUID();
        VideoUploadedEvent event = new VideoUploadedEvent(videoId, "user-1",
                "videos/user-1/uuid/video.mp4", "video.mp4", 1024L, "video/mp4", "2025-01-01T10:00:00Z");

        Path fakeVideo = tempDir.resolve("video.mp4");
        Files.writeString(fakeVideo, "fake");
        when(downloadPort.download(anyString())).thenReturn(fakeVideo);
        when(ffmpegPort.extractFrames(any(), any())).thenThrow(new FfmpegProcessingException("exit code 1"));

        VideoJobResult result = useCase.execute(event);

        assertThat(result.getStatus()).isEqualTo(VideoJobStatus.FAILED);
        assertThat(result.getErrorMessage()).contains("exit code 1");
        verify(eventPublisher).publishFailed(eq(videoId), eq("user-1"), anyString());
        verify(eventPublisher, never()).publishProcessed(any());
    }

    @Test
    void shouldPublishFailedEventWhenDownloadFails() {
        UUID videoId = UUID.randomUUID();
        VideoUploadedEvent event = new VideoUploadedEvent(videoId, "user-1",
                "videos/user-1/uuid/video.mp4", "video.mp4", 1024L, "video/mp4", "2025-01-01T10:00:00Z");

        when(downloadPort.download(anyString())).thenThrow(new RuntimeException("S3 unavailable"));

        VideoJobResult result = useCase.execute(event);

        assertThat(result.getStatus()).isEqualTo(VideoJobStatus.FAILED);
        verify(eventPublisher).publishFailed(eq(videoId), eq("user-1"), anyString());
    }
}
