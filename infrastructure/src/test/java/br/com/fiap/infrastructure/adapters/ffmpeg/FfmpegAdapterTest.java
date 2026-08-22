package br.com.fiap.infrastructure.adapters.ffmpeg;

import br.com.fiap.domain.exceptions.FfmpegProcessingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class FfmpegAdapterTest {

    private final FfmpegAdapter adapter = new FfmpegAdapter(1L);

    @AfterEach
    void clearInterrupt() {
        Thread.interrupted();
    }

    @Test
    void shouldThrowExceptionWhenFfmpegFails(@TempDir Path tempDir) throws IOException {
        Path fakeVideo = tempDir.resolve("fake.mp4");
        Files.writeString(fakeVideo, "not a real video");
        Path framesDir = tempDir.resolve("frames");
        Files.createDirectories(framesDir);

        assertThatThrownBy(() -> adapter.extractFrames(fakeVideo, framesDir))
                .isInstanceOf(FfmpegProcessingException.class);
    }

    @Test
    void shouldExtractFramesSuccessfully(@TempDir Path tempDir) throws Exception {
        Path videoPath = tempDir.resolve("video.mp4");
        Files.writeString(videoPath, "fake-video");
        Path framesDir = tempDir.resolve("frames");
        Files.createDirectories(framesDir);
        Files.writeString(framesDir.resolve("frame_0001.png"), "png1");
        Files.writeString(framesDir.resolve("frame_0002.png"), "png2");

        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(mockProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);

        try (MockedConstruction<ProcessBuilder> ignored = mockConstruction(ProcessBuilder.class,
                (pb, ctx) -> when(pb.start()).thenReturn(mockProcess))) {
            int frames = adapter.extractFrames(videoPath, framesDir);
            assertThat(frames).isEqualTo(2);
        }
    }

    @Test
    void shouldThrowWhenFfmpegTimesOut(@TempDir Path tempDir) throws Exception {
        Path videoPath = tempDir.resolve("video.mp4");
        Files.writeString(videoPath, "fake-video");
        Path framesDir = tempDir.resolve("frames");
        Files.createDirectories(framesDir);

        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(mockProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(false);

        try (MockedConstruction<ProcessBuilder> ignored = mockConstruction(ProcessBuilder.class,
                (pb, ctx) -> when(pb.start()).thenReturn(mockProcess))) {
            assertThatThrownBy(() -> adapter.extractFrames(videoPath, framesDir))
                    .isInstanceOf(FfmpegProcessingException.class)
                    .hasMessageContaining("timed out");
        }
    }

    @Test
    void shouldThrowWhenFfmpegExitsWithNonZeroCode(@TempDir Path tempDir) throws Exception {
        Path videoPath = tempDir.resolve("video.mp4");
        Files.writeString(videoPath, "fake-video");
        Path framesDir = tempDir.resolve("frames");
        Files.createDirectories(framesDir);

        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(mockProcess.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(1);

        try (MockedConstruction<ProcessBuilder> ignored = mockConstruction(ProcessBuilder.class,
                (pb, ctx) -> when(pb.start()).thenReturn(mockProcess))) {
            assertThatThrownBy(() -> adapter.extractFrames(videoPath, framesDir))
                    .isInstanceOf(FfmpegProcessingException.class)
                    .hasMessageContaining("exited with code");
        }
    }

    @Test
    void shouldThrowWhenFfmpegIsInterrupted(@TempDir Path tempDir) throws Exception {
        Path videoPath = tempDir.resolve("video.mp4");
        Files.writeString(videoPath, "fake-video");
        Path framesDir = tempDir.resolve("frames");
        Files.createDirectories(framesDir);

        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(mockProcess.waitFor(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException("test-interrupt"));

        try (MockedConstruction<ProcessBuilder> ignored = mockConstruction(ProcessBuilder.class,
                (pb, ctx) -> when(pb.start()).thenReturn(mockProcess))) {
            assertThatThrownBy(() -> adapter.extractFrames(videoPath, framesDir))
                    .isInstanceOf(FfmpegProcessingException.class)
                    .hasMessageContaining("interrupted");
        }
    }
}
