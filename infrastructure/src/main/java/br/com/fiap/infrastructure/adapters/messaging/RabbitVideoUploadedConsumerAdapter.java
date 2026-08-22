package br.com.fiap.infrastructure.adapters.messaging;

import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.model.VideoUploadedEvent;
import br.com.fiap.domain.ports.in.ProcessVideoInputPort;
import br.com.fiap.infrastructure.configuration.RabbitMqConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Deque;

/** Consome eventos video-uploaded do RabbitMQ e dispara o processamento. */
@Component
public class RabbitVideoUploadedConsumerAdapter {

    private static final Logger log = LoggerFactory.getLogger(RabbitVideoUploadedConsumerAdapter.class);

    private final ProcessVideoInputPort processVideo;
    private final ObjectMapper objectMapper;
    private final Deque<VideoJobResult> jobHistory;

    public RabbitVideoUploadedConsumerAdapter(ProcessVideoInputPort processVideo,
                                              ObjectMapper objectMapper,
                                              Deque<VideoJobResult> jobHistory) {
        this.processVideo = processVideo;
        this.objectMapper = objectMapper;
        this.jobHistory = jobHistory;
    }

    @RabbitListener(queues = RabbitMqConfiguration.QUEUE_VIDEO_UPLOADED)
    public void onVideoUploaded(String message) {
        try {
            // Defensive unwrap: some publishers double-serialize the payload
            // (the JSON object is wrapped in an outer JSON string).
            JsonNode root = objectMapper.readTree(message);
            String json = root.isTextual() ? root.asText() : message;

            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(json));
            log.info(">>> Payload recebido do RabbitMQ — fila [{}]:\n{}",
                    RabbitMqConfiguration.QUEUE_VIDEO_UPLOADED, prettyJson);

            VideoUploadedEvent event = objectMapper.readValue(json, VideoUploadedEvent.class);

            MDC.put("videoId", event.videoId().toString());
            MDC.put("userId", event.userId());
            MDC.put("processingStage", "MESSAGE_RECEIVED");

            log.info(">>> Consumindo tópico [{}] — arquivo: \"{}\" | userId: {} | videoId: {}",
                    RabbitMqConfiguration.QUEUE_VIDEO_UPLOADED,
                    event.originalFilename(), event.userId(), event.videoId());

            MDC.put("processingStage", "PROCESSING");
            long start = System.currentTimeMillis();

            VideoJobResult result = processVideo.execute(event);

            long durationMs = System.currentTimeMillis() - start;
            MDC.put("processingStage", "COMPLETED");
            MDC.put("durationMs", String.valueOf(durationMs));

            log.info("<<< Job finalizado — arquivo: \"{}\" | status: {} | duração: {}",
                    event.originalFilename(), result.getStatus(), formatDuration(durationMs));

            addToHistory(result);
        } catch (Exception e) {
            MDC.put("processingStage", "FAILED");
            log.error("<<< Falha ao consumir mensagem do tópico [{}]: {}",
                    RabbitMqConfiguration.QUEUE_VIDEO_UPLOADED, e.getMessage(), e);
            throw new RuntimeException("Failed to process video-uploaded message", e);
        } finally {
            MDC.remove("videoId");
            MDC.remove("userId");
            MDC.remove("processingStage");
            MDC.remove("durationMs");
        }
    }

    private static String formatDuration(long ms) {
        return ms < 1000 ? ms + "ms" : String.format("%.1fs", ms / 1000.0);
    }

    private void addToHistory(VideoJobResult result) {
        jobHistory.addFirst(result);
        int maxHistory = 100;
        while (jobHistory.size() > maxHistory) {
            jobHistory.pollLast();
        }
    }
}

