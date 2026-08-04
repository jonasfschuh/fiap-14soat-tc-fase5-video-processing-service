package br.com.fiap.infrastructure.adapters.storage;

import br.com.fiap.domain.exceptions.VideoDownloadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileDownloadAdapterTest {

    @Test
    void shouldDownloadFileSuccessfully(@TempDir Path tempDir) throws IOException {
        Path videoFile = tempDir.resolve("video.mp4");
        Files.writeString(videoFile, "fake-video-content");

        LocalFileDownloadAdapter adapter = new LocalFileDownloadAdapter(tempDir.toString());
        Path downloaded = adapter.download("video.mp4");

        assertThat(downloaded).exists();
        assertThat(Files.readString(downloaded)).isEqualTo("fake-video-content");
    }

    @Test
    void shouldThrowVideoDownloadExceptionWhenFileNotFound(@TempDir Path tempDir) {
        LocalFileDownloadAdapter adapter = new LocalFileDownloadAdapter(tempDir.toString());

        assertThatThrownBy(() -> adapter.download("nonexistent.mp4"))
                .isInstanceOf(VideoDownloadException.class);
    }
}
