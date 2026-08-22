package br.com.fiap.infrastructure.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RabbitMqConfigurationTest {

    private final RabbitMqConfiguration config = new RabbitMqConfiguration();

    @Test
    void shouldDeclareDurableTopicExchangeAndQueues() {
        TopicExchange exchange = config.videoEventsExchange();
        Queue uploadedQueue = config.videoUploadedQueue();
        Queue uploadedDlq = config.videoUploadedDlq();
        Queue eventsQueue = config.videoEventsQueue();
        Queue eventsDlq = config.videoEventsDlq();

        assertThat(exchange.getName()).isEqualTo(RabbitMqConfiguration.EXCHANGE_VIDEO_EVENTS);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.getType()).isEqualTo("topic");
        assertThat(uploadedQueue.getName()).isEqualTo(RabbitMqConfiguration.QUEUE_VIDEO_UPLOADED);
        assertThat(uploadedQueue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitMqConfiguration.EXCHANGE_VIDEO_EVENTS)
                .containsEntry("x-dead-letter-routing-key", RabbitMqConfiguration.ROUTING_VIDEO_UPLOADED_DLQ);
        assertThat(uploadedDlq.getName()).isEqualTo(RabbitMqConfiguration.QUEUE_VIDEO_UPLOADED_DLQ);
        assertThat(eventsQueue.getName()).isEqualTo(RabbitMqConfiguration.QUEUE_VIDEO_EVENTS);
        assertThat(eventsQueue.getArguments())
                .containsEntry("x-dead-letter-exchange", RabbitMqConfiguration.EXCHANGE_VIDEO_EVENTS)
                .containsEntry("x-dead-letter-routing-key", RabbitMqConfiguration.ROUTING_VIDEO_EVENTS_DLQ);
        assertThat(eventsDlq.getName()).isEqualTo(RabbitMqConfiguration.QUEUE_VIDEO_EVENTS_DLQ);
    }

    @Test
    void shouldDeclareExpectedBindingsAndPublisherBean() {
        TopicExchange exchange = config.videoEventsExchange();
        Binding uploadedBinding = config.videoUploadedBinding(config.videoUploadedQueue(), exchange);
        Binding uploadedDlqBinding = config.videoUploadedDlqBinding(config.videoUploadedDlq(), exchange);
        Binding processedBinding = config.videoProcessedBinding(config.videoEventsQueue(), exchange);
        Binding failedBinding = config.videoFailedBinding(config.videoEventsQueue(), exchange);
        Binding eventsDlqBinding = config.videoEventsDlqBinding(config.videoEventsDlq(), exchange);

        assertThat(uploadedBinding.getRoutingKey()).isEqualTo(RabbitMqConfiguration.ROUTING_VIDEO_UPLOADED);
        assertThat(uploadedDlqBinding.getRoutingKey()).isEqualTo(RabbitMqConfiguration.ROUTING_VIDEO_UPLOADED_DLQ);
        assertThat(processedBinding.getRoutingKey()).isEqualTo(RabbitMqConfiguration.ROUTING_VIDEO_PROCESSED);
        assertThat(failedBinding.getRoutingKey()).isEqualTo(RabbitMqConfiguration.ROUTING_VIDEO_FAILED);
        assertThat(eventsDlqBinding.getRoutingKey()).isEqualTo(RabbitMqConfiguration.ROUTING_VIDEO_EVENTS_DLQ);
        assertThat(config.videoProcessingEventPublisherPort(mock(RabbitTemplate.class), new ObjectMapper(), "/tmp/processed", "/tmp/uploads")).isNotNull();
    }
}

