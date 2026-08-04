package br.com.fiap.infrastructure.adapters.messaging;

import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.model.VideoJobStatus;
import br.com.fiap.infrastructure.logging.SqsMessageLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqsVideoProcessingEventPublisherAdapterTest {

    @Test
    void shouldPublishProcessedEvent() {
        SqsClient sqsClient = mock(SqsClient.class);
        when(sqsClient.sendMessage(org.mockito.ArgumentMatchers.any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("1").build());
        SqsMessageLogger logger = mock(SqsMessageLogger.class);

        SqsVideoProcessingEventPublisherAdapter adapter = new SqsVideoProcessingEventPublisherAdapter(
                sqsClient, new ObjectMapper(), logger, "http://localhost/video-events");

        VideoJobResult result = VideoJobResult.builder(UUID.randomUUID(), "user-1")
                .status(VideoJobStatus.DONE)
                .frameCount(3)
                .outputKey("outputs/user-1/video-1/frames.zip")
                .build();

        adapter.publishProcessed(result);

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        assertThat(captor.getValue().queueUrl()).isEqualTo("http://localhost/video-events");
        assertThat(captor.getValue().messageBody()).contains("\"eventType\":\"VIDEO_PROCESSED\"");
        verify(logger).logMessageSent(eq("http://localhost/video-events"),
                eq(captor.getValue().messageBody()), eq("VIDEO_PROCESSED"), isNull());
    }

    @Test
    void shouldPublishFailedEvent() {
        SqsClient sqsClient = mock(SqsClient.class);
        when(sqsClient.sendMessage(org.mockito.ArgumentMatchers.any(SendMessageRequest.class)))
                .thenReturn(SendMessageResponse.builder().messageId("1").build());
        SqsMessageLogger logger = mock(SqsMessageLogger.class);

        SqsVideoProcessingEventPublisherAdapter adapter = new SqsVideoProcessingEventPublisherAdapter(
                sqsClient, new ObjectMapper(), logger, "http://localhost/video-events");

        UUID videoId = UUID.randomUUID();
        adapter.publishFailed(videoId, "user-1", "boom");

        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        assertThat(captor.getValue().messageBody()).contains("\"eventType\":\"VIDEO_FAILED\"");
        assertThat(captor.getValue().messageBody()).contains(videoId.toString());
        verify(logger).logMessageSent(eq("http://localhost/video-events"),
                eq(captor.getValue().messageBody()), eq("VIDEO_FAILED"), isNull());
    }
}
