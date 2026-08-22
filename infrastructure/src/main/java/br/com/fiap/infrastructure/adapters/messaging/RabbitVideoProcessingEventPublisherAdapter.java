package br.com.fiap.infrastructure.adapters.messaging;

import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.ports.out.VideoProcessingEventPublisherPort;
import br.com.fiap.infrastructure.configuration.RabbitMqConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Publica eventos de resultado do processamento no RabbitMQ. */
public class RabbitVideoProcessingEventPublisherAdapter implements VideoProcessingEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitVideoProcessingEventPublisherAdapter.class);
    private static final String EXCHANGE = RabbitMqConfiguration.EXCHANGE_VIDEO_EVENTS;

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public RabbitVideoProcessingEventPublisherAdapter(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishProcessed(VideoJobResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "VIDEO_PROCESSED");
        payload.put("videoId", result.getVideoId().toString());
        payload.put("userId", result.getUserId());
        payload.put("outputKey", result.getOutputKey());
        payload.put("frameCount", result.getFrameCount());
        payload.put("status", "DONE");
        payload.put("timestamp", Instant.now().toString());
        log.info(">>> Publicando no tópico [{}] routing-key [video.processed] — artefato: {} | frames: {}",
                EXCHANGE, result.getOutputKey(), result.getFrameCount());
        send(payload, "video.processed");
    }

    @Override
    public void publishFailed(UUID videoId, String userId, String errorMessage) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "VIDEO_FAILED");
        payload.put("videoId", videoId.toString());
        payload.put("userId", userId);
        payload.put("errorMessage", errorMessage);
        payload.put("status", "FAILED");
        payload.put("timestamp", Instant.now().toString());
        log.info(">>> Publicando no tópico [{}] routing-key [video.failed] — videoId: {} | motivo: {}",
                EXCHANGE, videoId, errorMessage);
        send(payload, "video.failed");
    }

    private void send(Map<String, Object> payload, String routingKey) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            String prettyBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            log.info(">>> Payload enviado ao RabbitMQ — exchange [{}] routing-key [{}]:\n{}",
                    EXCHANGE, routingKey, prettyBody);
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, body);
            log.info("<<< Publicado com sucesso no tópico [{}] routing-key [{}]", EXCHANGE, routingKey);
        } catch (Exception e) {
            log.error("<<< Falha ao publicar no tópico [{}] routing-key [{}]: {}",
                    EXCHANGE, routingKey, e.getMessage(), e);
            throw new RuntimeException("Failed to publish " + routingKey, e);
        }
    }
}

