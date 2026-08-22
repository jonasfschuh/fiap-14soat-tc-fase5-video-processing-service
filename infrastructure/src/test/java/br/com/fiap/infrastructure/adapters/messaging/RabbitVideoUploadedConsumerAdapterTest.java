package br.com.fiap.infrastructure.adapters.messaging;

import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.model.VideoJobStatus;
import br.com.fiap.domain.model.VideoUploadedEvent;
import br.com.fiap.domain.ports.in.ProcessVideoInputPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RabbitVideoUploadedConsumerAdapterTest {

    @Test
    void shouldProcessDoubleSerializedMessage() throws Exception {
        ProcessVideoInputPort processVideo = mock(ProcessVideoInputPort.class);
        Deque<VideoJobResult> history = new ConcurrentLinkedDeque<>();

        VideoUploadedEvent event = new VideoUploadedEvent(UUID.randomUUID(), "user-1",
                "videos/user-1/video.mp4", "video.mp4", 123L, "video/mp4", "2025-01-01T10:00:00Z");
        VideoJobResult result = VideoJobResult.builder(event.videoId(), event.userId())
                .status(VideoJobStatus.DONE)
                .frameCount(2)
                .outputKey("outputs/user-1/video/frames.zip")
                .build();
        when(processVideo.execute(any(VideoUploadedEvent.class))).thenReturn(result);

        RabbitVideoUploadedConsumerAdapter adapter = new RabbitVideoUploadedConsumerAdapter(
                processVideo, new ObjectMapper(), history);

        // Payload real: JSON do objeto serializado duas vezes (como chega do publisher)
        String innerJson = new ObjectMapper().writeValueAsString(event);
        String doubleSerializedMessage = new ObjectMapper().writeValueAsString(innerJson);
        adapter.onVideoUploaded(doubleSerializedMessage);

        assertThat(history).hasSize(1);
        verify(processVideo).execute(event);
    }

    @Test
    void shouldProcessUploadedMessageAndStoreHistory() throws Exception {
        ProcessVideoInputPort processVideo = mock(ProcessVideoInputPort.class);
        Deque<VideoJobResult> history = new ConcurrentLinkedDeque<>();

        VideoUploadedEvent event = new VideoUploadedEvent(UUID.randomUUID(), "user-1",
                "videos/user-1/video.mp4", "video.mp4", 123L, "video/mp4", "2025-01-01T10:00:00Z");
        VideoJobResult result = VideoJobResult.builder(event.videoId(), event.userId())
                .status(VideoJobStatus.DONE)
                .frameCount(2)
                .outputKey("outputs/user-1/video/frames.zip")
                .build();
        when(processVideo.execute(any(VideoUploadedEvent.class))).thenReturn(result);

        RabbitVideoUploadedConsumerAdapter adapter = new RabbitVideoUploadedConsumerAdapter(
                processVideo, new ObjectMapper(), history);

        adapter.onVideoUploaded(new ObjectMapper().writeValueAsString(event));

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().getStatus()).isEqualTo(VideoJobStatus.DONE);
        verify(processVideo).execute(event);
    }

    @Test
    void shouldThrowWhenMessageIsInvalid() {
        ProcessVideoInputPort processVideo = mock(ProcessVideoInputPort.class);
        RabbitVideoUploadedConsumerAdapter adapter = new RabbitVideoUploadedConsumerAdapter(
                processVideo, new ObjectMapper(), new ConcurrentLinkedDeque<>());

        assertThatThrownBy(() -> adapter.onVideoUploaded("invalid-json"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to process video-uploaded message");

        verifyNoInteractions(processVideo);
    }

    @Test
    void shouldTrimHistoryWhenExceedsMaximum() throws Exception {
        ProcessVideoInputPort processVideo = mock(ProcessVideoInputPort.class);
        Deque<VideoJobResult> history = new ConcurrentLinkedDeque<>();

        VideoUploadedEvent event = new VideoUploadedEvent(UUID.randomUUID(), "user-1",
                "videos/user-1/video.mp4", "video.mp4", 123L, "video/mp4", "2025-01-01T10:00:00Z");
        VideoJobResult result = VideoJobResult.builder(event.videoId(), event.userId())
                .status(VideoJobStatus.DONE)
                .frameCount(2)
                .outputKey("outputs/user-1/video/frames.zip")
                .build();
        when(processVideo.execute(any())).thenReturn(result);

        RabbitVideoUploadedConsumerAdapter adapter = new RabbitVideoUploadedConsumerAdapter(
                processVideo, new ObjectMapper(), history);

        String message = new ObjectMapper().writeValueAsString(event);
        for (int i = 0; i < 105; i++) {
            adapter.onVideoUploaded(message);
        }

        assertThat(history).hasSize(100);
    }
}

