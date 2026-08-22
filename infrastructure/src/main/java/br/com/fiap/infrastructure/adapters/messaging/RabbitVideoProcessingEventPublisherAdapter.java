package br.com.fiap.infrastructure.adapters.messaging;

import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.ports.out.VideoProcessingEventPublisherPort;
import br.com.fiap.infrastructure.configuration.RabbitMqConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Publica eventos de resultado do processamento no RabbitMQ. */
public class RabbitVideoProcessingEventPublisherAdapter implements VideoProcessingEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(RabbitVideoProcessingEventPublisherAdapter.class);
    private static final String EXCHANGE = RabbitMqConfiguration.EXCHANGE_VIDEO_EVENTS;
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final String outputBasePath;
    private final String inputBasePath;

    public RabbitVideoProcessingEventPublisherAdapter(RabbitTemplate rabbitTemplate,
                                                     ObjectMapper objectMapper,
                                                     String outputBasePath,
                                                     String inputBasePath) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.outputBasePath = outputBasePath;
        this.inputBasePath = inputBasePath;
    }

    @Override
    public void publishProcessed(VideoJobResult result) {
        String outputAbsolutePath = Paths.get(outputBasePath, result.getOutputKey()).toAbsolutePath().toString();
        String storageAbsolutePath = result.getStorageKey() != null
                ? Paths.get(inputBasePath, result.getStorageKey()).toAbsolutePath().toString()
                : null;
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "VIDEO_PROCESSED");
        payload.put("videoId", result.getVideoId().toString());
        payload.put("userId", result.getUserId());
        payload.put("originalFilename", result.getOriginalFilename());
        payload.put("mimeType", result.getMimeType());
        payload.put("fileSizeBytes", result.getFileSizeBytes());
        payload.put("storageAbsolutePath", storageAbsolutePath);
        payload.put("outputKey", result.getOutputKey());
        payload.put("outputAbsolutePath", outputAbsolutePath);
        payload.put("frameCount", result.getFrameCount());
        payload.put("status", "DONE");
        payload.put("timestamp", ZonedDateTime.now(SAO_PAULO).format(TIMESTAMP_FMT));
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
        payload.put("timestamp", ZonedDateTime.now(SAO_PAULO).format(TIMESTAMP_FMT));
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

