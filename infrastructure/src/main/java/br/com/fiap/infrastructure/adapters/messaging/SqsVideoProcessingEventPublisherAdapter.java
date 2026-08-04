package br.com.fiap.infrastructure.adapters.messaging;

import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.ports.out.VideoProcessingEventPublisherPort;
import br.com.fiap.infrastructure.logging.SqsMessageLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class SqsVideoProcessingEventPublisherAdapter implements VideoProcessingEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(SqsVideoProcessingEventPublisherAdapter.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final SqsMessageLogger sqsMessageLogger;
    private final String queueUrl;

    public SqsVideoProcessingEventPublisherAdapter(SqsClient sqsClient,
                                                   ObjectMapper objectMapper,
                                                   SqsMessageLogger sqsMessageLogger,
                                                   String queueUrl) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.sqsMessageLogger = sqsMessageLogger;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publishProcessed(VideoJobResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "VIDEO_PROCESSED");
        payload.put("videoId", result.getVideoId().toString());
        payload.put("userId", result.getUserId());
        payload.put("outputKey", result.getOutputKey());
        payload.put("frameCount", result.getFrameCount());
        payload.put("timestamp", Instant.now().toString());
        send(payload, "VIDEO_PROCESSED");
    }

    @Override
    public void publishFailed(UUID videoId, String userId, String errorMessage) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "VIDEO_FAILED");
        payload.put("videoId", videoId.toString());
        payload.put("userId", userId);
        payload.put("errorMessage", errorMessage);
        payload.put("timestamp", Instant.now().toString());
        send(payload, "VIDEO_FAILED");
    }

    private void send(Map<String, Object> payload, String eventType) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(body)
                    .build());
            sqsMessageLogger.logMessageSent(queueUrl, body, eventType, null);
            log.info("[SQS-PUBLISHER] Published {} event", eventType);
        } catch (Exception e) {
            log.error("[SQS-PUBLISHER] Failed to publish {} event", eventType, e);
            throw new RuntimeException("Failed to publish " + eventType, e);
        }
    }
}
