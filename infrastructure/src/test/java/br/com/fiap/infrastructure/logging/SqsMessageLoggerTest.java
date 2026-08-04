package br.com.fiap.infrastructure.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("SqsMessageLogger - Unit Tests")
class SqsMessageLoggerTest {

    private SqsMessageLogger logger;

    @BeforeEach
    void setUp() {
        logger = new SqsMessageLogger(new ObjectMapper());
    }

    @Test
    @DisplayName("logMessageSent with valid JSON body does not throw")
    void logMessageSent_validJson_doesNotThrow() {
        String body = "{\"key\":\"value\"}";
        Map<String, Object> metadata = Map.of("sagaId", "saga-1", "serviceOrderId", "SO-1");

        assertThatCode(() -> logger.logMessageSent("http://queue-url", body, "TEST_EVENT", metadata))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logMessageSent with plain string body does not throw")
    void logMessageSent_plainStringBody_doesNotThrow() {
        assertThatCode(() -> logger.logMessageSent("http://queue-url", "plain text", "TEST_EVENT", null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logMessageSent with null metadata does not throw")
    void logMessageSent_nullMetadata_doesNotThrow() {
        assertThatCode(() -> logger.logMessageSent("http://queue-url", "{}", "EVENT", null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logMessageSent with empty metadata does not throw")
    void logMessageSent_emptyMetadata_doesNotThrow() {
        assertThatCode(() -> logger.logMessageSent("http://queue-url", "{}", "EVENT", Map.of()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logMessageReceived with valid JSON body does not throw")
    void logMessageReceived_validJson_doesNotThrow() {
        String body = "{\"sagaId\":\"saga-1\",\"commandType\":\"VIDEO_UPLOAD_COMMAND\"}";

        assertThatCode(() -> logger.logMessageReceived("http://queue-url", body, "msg-id-1", "receipt-1", null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logMessageReceived with plain string body does not throw")
    void logMessageReceived_plainStringBody_doesNotThrow() {
        assertThatCode(() -> logger.logMessageReceived("http://queue-url", "plain text", "msg-id", "receipt", null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("logMessageReceived with metadata does not throw")
    void logMessageReceived_withMetadata_doesNotThrow() {
        Map<String, Object> metadata = Map.of("sagaId", "saga-1");

        assertThatCode(() -> logger.logMessageReceived("http://queue-url", "{}", "msg-id", "receipt", metadata))
                .doesNotThrowAnyException();
    }
}
