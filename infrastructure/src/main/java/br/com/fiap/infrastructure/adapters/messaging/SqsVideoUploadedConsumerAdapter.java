package br.com.fiap.infrastructure.adapters.messaging;

import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.model.VideoUploadedEvent;
import br.com.fiap.domain.ports.in.ProcessVideoInputPort;
import br.com.fiap.infrastructure.logging.SqsMessageLogger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.Deque;

@Component
@ConditionalOnProperty(name = "aws.sqs.enabled", havingValue = "true", matchIfMissing = false)
public class SqsVideoUploadedConsumerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SqsVideoUploadedConsumerAdapter.class);

    private final SqsClient sqsClient;
    private final ProcessVideoInputPort processVideo;
    private final ObjectMapper objectMapper;
    private final SqsMessageLogger sqsMessageLogger;
    private final Deque<VideoJobResult> jobHistory;

    @Value("${aws.sqs.queues.video-uploaded:http://localhost:4566/000000000000/video-uploaded}")
    private String videoUploadedQueueUrl;

    @Value("${app.processing.jobs.max-history:100}")
    private int maxHistory;

    public SqsVideoUploadedConsumerAdapter(SqsClient sqsClient,
                                           ProcessVideoInputPort processVideo,
                                           ObjectMapper objectMapper,
                                           SqsMessageLogger sqsMessageLogger,
                                           Deque<VideoJobResult> jobHistory) {
        this.sqsClient = sqsClient;
        this.processVideo = processVideo;
        this.objectMapper = objectMapper;
        this.sqsMessageLogger = sqsMessageLogger;
        this.jobHistory = jobHistory;
    }

    @Scheduled(fixedDelayString = "${aws.sqs.polling.fixed-delay-ms:3000}")
    public void pollVideoUploaded() {
        try {
            ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                    .queueUrl(videoUploadedQueueUrl)
                    .maxNumberOfMessages(10)
                    .waitTimeSeconds(5)
                    .build());

            for (Message message : response.messages()) {
                processMessage(message);
            }
        } catch (Exception e) {
            log.error("[SQS-CONSUMER] Error polling video-uploaded queue", e);
        }
    }

    private void processMessage(Message message) {
        try {
            sqsMessageLogger.logMessageReceived(videoUploadedQueueUrl, message.body(),
                    message.messageId(), message.receiptHandle(), null);

            VideoUploadedEvent event = objectMapper.readValue(message.body(), VideoUploadedEvent.class);
            log.info("[SQS-CONSUMER] Processing videoId={} userId={}", event.videoId(), event.userId());

            VideoJobResult result = processVideo.execute(event);
            addToHistory(result);

            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(videoUploadedQueueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build());

            log.info("[SQS-CONSUMER] Completed videoId={} status={}", event.videoId(), result.getStatus());
        } catch (Exception ex) {
            log.error("[SQS-CONSUMER] Unhandled error processing message id={}: {}",
                    message.messageId(), ex.getMessage(), ex);
        }
    }

    private void addToHistory(VideoJobResult result) {
        jobHistory.addFirst(result);
        while (jobHistory.size() > maxHistory) {
            jobHistory.pollLast();
        }
    }
}
