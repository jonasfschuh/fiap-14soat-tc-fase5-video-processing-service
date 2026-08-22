package br.com.fiap.infrastructure.configuration;

import br.com.fiap.domain.ports.out.VideoProcessingEventPublisherPort;
import br.com.fiap.infrastructure.adapters.messaging.RabbitVideoProcessingEventPublisherAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfiguration {

    public static final String EXCHANGE_VIDEO_EVENTS = "video.events";
    public static final String QUEUE_VIDEO_UPLOADED = "video-uploaded";
    public static final String QUEUE_VIDEO_UPLOADED_DLQ = "video-uploaded-dlq";
    public static final String QUEUE_VIDEO_EVENTS = "video-events";
    public static final String QUEUE_VIDEO_EVENTS_DLQ = "video-events-dlq";
    public static final String ROUTING_VIDEO_UPLOADED = "video.uploaded";
    public static final String ROUTING_VIDEO_PROCESSED = "video.processed";
    public static final String ROUTING_VIDEO_FAILED = "video.failed";
    public static final String ROUTING_VIDEO_UPLOADED_DLQ = "video.uploaded.dlq";
    public static final String ROUTING_VIDEO_EVENTS_DLQ = "video.events.dlq";

    @Bean
    public TopicExchange videoEventsExchange() {
        return new TopicExchange(EXCHANGE_VIDEO_EVENTS, true, false);
    }

    @Bean
    public Queue videoUploadedQueue() {
        return QueueBuilder.durable(QUEUE_VIDEO_UPLOADED)
                .deadLetterExchange(EXCHANGE_VIDEO_EVENTS)
                .deadLetterRoutingKey(ROUTING_VIDEO_UPLOADED_DLQ)
                .build();
    }

    @Bean
    public Queue videoUploadedDlq() {
        return QueueBuilder.durable(QUEUE_VIDEO_UPLOADED_DLQ).build();
    }

    @Bean
    public Queue videoEventsQueue() {
        return QueueBuilder.durable(QUEUE_VIDEO_EVENTS)
                .deadLetterExchange(EXCHANGE_VIDEO_EVENTS)
                .deadLetterRoutingKey(ROUTING_VIDEO_EVENTS_DLQ)
                .build();
    }

    @Bean
    public Queue videoEventsDlq() {
        return QueueBuilder.durable(QUEUE_VIDEO_EVENTS_DLQ).build();
    }

    @Bean
    public Binding videoUploadedBinding(Queue videoUploadedQueue, TopicExchange videoEventsExchange) {
        return BindingBuilder.bind(videoUploadedQueue).to(videoEventsExchange).with(ROUTING_VIDEO_UPLOADED);
    }

    @Bean
    public Binding videoUploadedDlqBinding(Queue videoUploadedDlq, TopicExchange videoEventsExchange) {
        return BindingBuilder.bind(videoUploadedDlq).to(videoEventsExchange).with(ROUTING_VIDEO_UPLOADED_DLQ);
    }

    @Bean
    public Binding videoProcessedBinding(Queue videoEventsQueue, TopicExchange videoEventsExchange) {
        return BindingBuilder.bind(videoEventsQueue).to(videoEventsExchange).with(ROUTING_VIDEO_PROCESSED);
    }

    @Bean
    public Binding videoFailedBinding(Queue videoEventsQueue, TopicExchange videoEventsExchange) {
        return BindingBuilder.bind(videoEventsQueue).to(videoEventsExchange).with(ROUTING_VIDEO_FAILED);
    }

    @Bean
    public Binding videoEventsDlqBinding(Queue videoEventsDlq, TopicExchange videoEventsExchange) {
        return BindingBuilder.bind(videoEventsDlq).to(videoEventsExchange).with(ROUTING_VIDEO_EVENTS_DLQ);
    }

    @Bean
    public VideoProcessingEventPublisherPort videoProcessingEventPublisherPort(
            RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        return new RabbitVideoProcessingEventPublisherAdapter(rabbitTemplate, objectMapper);
    }
}

