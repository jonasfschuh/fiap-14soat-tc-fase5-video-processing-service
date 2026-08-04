package br.com.fiap.domain.unit.model;

import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.model.VideoJobStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VideoJobResultTest {

    @Test
    void shouldBuildDoneResult() {
        UUID id = UUID.randomUUID();

        VideoJobResult result = VideoJobResult.builder(id, "user-1")
                .outputKey("outputs/user-1/uuid/frames.zip")
                .frameCount(42)
                .status(VideoJobStatus.DONE)
                .build();

        assertThat(result.getVideoId()).isEqualTo(id);
        assertThat(result.getUserId()).isEqualTo("user-1");
        assertThat(result.getStatus()).isEqualTo(VideoJobStatus.DONE);
        assertThat(result.getFrameCount()).isEqualTo(42);
        assertThat(result.getProcessedAt()).isNotNull();
    }

    @Test
    void shouldBuildFailedResult() {
        UUID id = UUID.randomUUID();

        VideoJobResult result = VideoJobResult.builder(id, "user-1")
                .status(VideoJobStatus.FAILED)
                .errorMessage("ffmpeg failed")
                .build();

        assertThat(result.getStatus()).isEqualTo(VideoJobStatus.FAILED);
        assertThat(result.getErrorMessage()).isEqualTo("ffmpeg failed");
        assertThat(result.getFrameCount()).isZero();
    }
}
