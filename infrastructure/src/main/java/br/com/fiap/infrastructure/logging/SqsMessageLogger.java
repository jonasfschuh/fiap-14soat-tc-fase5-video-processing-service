package br.com.fiap.infrastructure.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized logging utility for SQS messages in ms-video-upload.
 * Logs all sent and received messages in structured JSON format (pretty print).
 */
@Component
public class SqsMessageLogger {

    private static final Logger log = LoggerFactory.getLogger(SqsMessageLogger.class);
    private final ObjectWriter prettyWriter;
    private final ObjectMapper objectMapper;

    public SqsMessageLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.prettyWriter = objectMapper.writerWithDefaultPrettyPrinter();
    }

    public void logMessageSent(String queueUrl, String messageBody, String eventType, Map<String, Object> metadata) {
        try {
            Map<String, Object> logEntry = new LinkedHashMap<>();
            logEntry.put("timestamp", Instant.now().toString());
            logEntry.put("direction", "SENT");
            logEntry.put("queueUrl", queueUrl);
            logEntry.put("eventType", eventType);
            logEntry.put("messageBody", parseJsonOrString(messageBody));
            if (metadata != null && !metadata.isEmpty()) {
                logEntry.put("metadata", metadata);
            }
            String jsonLog = prettyWriter.writeValueAsString(logEntry);
            log.info("[SQS-SEND-MESSAGE] {}", jsonLog);
        } catch (Exception e) {
            log.error("[SQS-SEND-MESSAGE] Failed to serialize sent message log", e);
        }
    }

    public void logMessageReceived(String queueUrl, String messageBody, String messageId, String receiptHandle, Map<String, Object> metadata) {
        try {
            Map<String, Object> logEntry = new LinkedHashMap<>();
            logEntry.put("timestamp", Instant.now().toString());
            logEntry.put("direction", "RECEIVED");
            logEntry.put("queueUrl", queueUrl);
            logEntry.put("messageId", messageId);
            logEntry.put("receiptHandle", receiptHandle);
            logEntry.put("messageBody", parseJsonOrString(messageBody));
            if (metadata != null && !metadata.isEmpty()) {
                logEntry.put("metadata", metadata);
            }
            String jsonLog = prettyWriter.writeValueAsString(logEntry);
            log.info("[SQS-RECEIVED-MESSAGE] {}", jsonLog);
        } catch (Exception e) {
            log.error("[SQS-RECEIVED-MESSAGE] Failed to serialize received message log", e);
        }
    }

    private Object parseJsonOrString(String body) {
        try {
            return objectMapper.readValue(body, Object.class);
        } catch (Exception e) {
            return body;
        }
    }
}
