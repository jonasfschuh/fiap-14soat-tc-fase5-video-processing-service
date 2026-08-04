package br.com.fiap.infrastructure.adapters.ffmpeg;

import br.com.fiap.domain.exceptions.FfmpegProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FfmpegAdapterTest {

    private final FfmpegAdapter adapter = new FfmpegAdapter(1L);

    @Test
    void shouldThrowExceptionWhenFfmpegFails(@TempDir Path tempDir) throws IOException {
        Path fakeVideo = tempDir.resolve("fake.mp4");
        Files.writeString(fakeVideo, "not a real video");
        Path framesDir = tempDir.resolve("frames");
        Files.createDirectories(framesDir);

        assertThatThrownBy(() -> adapter.extractFrames(fakeVideo, framesDir))
                .isInstanceOf(FfmpegProcessingException.class);
    }
}
