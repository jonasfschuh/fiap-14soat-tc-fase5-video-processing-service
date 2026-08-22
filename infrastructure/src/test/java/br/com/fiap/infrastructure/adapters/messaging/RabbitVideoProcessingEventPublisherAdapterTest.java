package br.com.fiap.infrastructure.adapters.messaging;

import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.model.VideoJobStatus;
import br.com.fiap.infrastructure.configuration.RabbitMqConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitVideoProcessingEventPublisherAdapterTest {

    @Test
    void shouldPublishProcessedEvent() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitVideoProcessingEventPublisherAdapter adapter = new RabbitVideoProcessingEventPublisherAdapter(
                rabbitTemplate, new ObjectMapper(), "/tmp/processed", "/tmp/uploads");

        VideoJobResult result = VideoJobResult.builder(UUID.randomUUID(), "user-1")
                .status(VideoJobStatus.DONE)
                .frameCount(3)
                .outputKey("outputs/user-1/video-1/frames.zip")
                .storageKey("videos/user-1/video.mp4")
                .build();

        adapter.publishProcessed(result);

        ArgumentCaptor<String> exchange = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(exchange.capture(), routingKey.capture(), body.capture());
        assertThat(exchange.getValue()).isEqualTo(RabbitMqConfiguration.EXCHANGE_VIDEO_EVENTS);
        assertThat(routingKey.getValue()).isEqualTo("video.processed");
        assertThat(body.getValue()).contains("\"eventType\":\"VIDEO_PROCESSED\"");
        assertThat(body.getValue()).contains("\"status\":\"DONE\"");
    }

    @Test
    void shouldPublishFailedEvent() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RabbitVideoProcessingEventPublisherAdapter adapter = new RabbitVideoProcessingEventPublisherAdapter(
                rabbitTemplate, new ObjectMapper(), "/tmp/processed", "/tmp/uploads");

        UUID videoId = UUID.randomUUID();
        adapter.publishFailed(videoId, "user-1", "boom");

        ArgumentCaptor<String> routingKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfiguration.EXCHANGE_VIDEO_EVENTS), routingKey.capture(), body.capture());
        assertThat(routingKey.getValue()).isEqualTo("video.failed");
        assertThat(body.getValue()).contains("\"eventType\":\"VIDEO_FAILED\"");
        assertThat(body.getValue()).contains(videoId.toString());
        assertThat(body.getValue()).contains("\"status\":\"FAILED\"");
    }

    @Test
    void shouldThrowWhenSerializationFails() throws Exception {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        doThrow(new com.fasterxml.jackson.core.JsonProcessingException("serialization error") {})
                .when(objectMapper).writeValueAsString(any());

        RabbitVideoProcessingEventPublisherAdapter adapter = new RabbitVideoProcessingEventPublisherAdapter(
                rabbitTemplate, objectMapper, "/tmp/processed", "/tmp/uploads");

        assertThatThrownBy(() -> adapter.publishFailed(UUID.randomUUID(), "user-1", "error"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to publish");
    }
}
