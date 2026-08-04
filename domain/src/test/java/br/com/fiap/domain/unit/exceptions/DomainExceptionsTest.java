package br.com.fiap.domain.unit.exceptions;

import br.com.fiap.domain.exceptions.FfmpegProcessingException;
import br.com.fiap.domain.exceptions.VideoDownloadException;
import br.com.fiap.domain.exceptions.VideoZipException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionsTest {

    @Test
    void videoDownloadExceptionShouldContainKey() {
        RuntimeException cause = new RuntimeException("network error");

        VideoDownloadException ex = new VideoDownloadException("videos/user/uuid/video.mp4", cause);

        assertThat(ex.getMessage()).contains("videos/user/uuid/video.mp4");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void ffmpegProcessingExceptionShouldContainMessage() {
        FfmpegProcessingException ex = new FfmpegProcessingException("exit code 1");
        assertThat(ex.getMessage()).isEqualTo("exit code 1");
    }

    @Test
    void videoZipExceptionShouldContainCause() {
        RuntimeException cause = new RuntimeException("disk full");

        VideoZipException ex = new VideoZipException("zip failed", cause);

        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
