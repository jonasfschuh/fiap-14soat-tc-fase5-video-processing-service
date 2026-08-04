package br.com.fiap.infrastructure.adapters.messaging;

import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.model.VideoJobStatus;
import br.com.fiap.domain.model.VideoUploadedEvent;
import br.com.fiap.domain.ports.in.ProcessVideoInputPort;
import br.com.fiap.infrastructure.logging.SqsMessageLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SqsVideoUploadedConsumerAdapterTest {

    @Test
    void shouldPollProcessAndDeleteMessages() throws Exception {
        SqsClient sqsClient = mock(SqsClient.class);
        ProcessVideoInputPort processVideo = mock(ProcessVideoInputPort.class);
        SqsMessageLogger logger = mock(SqsMessageLogger.class);
        Deque<VideoJobResult> history = new ConcurrentLinkedDeque<>();

        VideoUploadedEvent event = new VideoUploadedEvent(UUID.randomUUID(), "user-1",
                "videos/user-1/video.mp4", "video.mp4", 123L, "video/mp4", "2025-01-01T10:00:00Z");
        String body = new ObjectMapper().writeValueAsString(event);

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder()
                        .messages(Message.builder()
                                .messageId("msg-1")
                                .receiptHandle("rh-1")
                                .body(body)
                                .build())
                        .build());
        when(processVideo.execute(any(VideoUploadedEvent.class)))
                .thenReturn(VideoJobResult.builder(event.videoId(), event.userId())
                        .status(VideoJobStatus.DONE)
                        .frameCount(2)
                        .outputKey("outputs/user-1/video/frames.zip")
                        .build());
        when(sqsClient.deleteMessage(org.mockito.ArgumentMatchers.any(DeleteMessageRequest.class)))
                .thenReturn(DeleteMessageResponse.builder().build());

        SqsVideoUploadedConsumerAdapter adapter = new SqsVideoUploadedConsumerAdapter(
                sqsClient, processVideo, new ObjectMapper(), logger, history);
        ReflectionTestUtils.setField(adapter, "videoUploadedQueueUrl", "http://localhost/video-uploaded");
        ReflectionTestUtils.setField(adapter, "maxHistory", 100);

        adapter.pollVideoUploaded();

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().getStatus()).isEqualTo(VideoJobStatus.DONE);
        verify(sqsClient).deleteMessage(argThat((DeleteMessageRequest request) ->
                request.queueUrl().equals("http://localhost/video-uploaded")
                        && request.receiptHandle().equals("rh-1")));
    }

    @Test
    void shouldKeepMessageWhenProcessingFails() {
        SqsClient sqsClient = mock(SqsClient.class);
        ProcessVideoInputPort processVideo = mock(ProcessVideoInputPort.class);
        SqsMessageLogger logger = mock(SqsMessageLogger.class);
        Deque<VideoJobResult> history = new ConcurrentLinkedDeque<>();

        when(sqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(ReceiveMessageResponse.builder()
                        .messages(Message.builder()
                                .messageId("msg-1")
                                .receiptHandle("rh-1")
                                .body("invalid-json")
                                .build())
                        .build());

        SqsVideoUploadedConsumerAdapter adapter = new SqsVideoUploadedConsumerAdapter(
                sqsClient, processVideo, new ObjectMapper(), logger, history);
        ReflectionTestUtils.setField(adapter, "videoUploadedQueueUrl", "http://localhost/video-uploaded");
        ReflectionTestUtils.setField(adapter, "maxHistory", 100);

        adapter.pollVideoUploaded();

        assertThat(history).isEmpty();
        verify(processVideo, never()).execute(any(VideoUploadedEvent.class));
        verify(sqsClient, never()).deleteMessage(org.mockito.ArgumentMatchers.any(DeleteMessageRequest.class));
    }
}
