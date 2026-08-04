package br.com.fiap.application.unit.controllers;

import br.com.fiap.application.adapters.JobStatusController;
import br.com.fiap.application.dtos.JobStatusResponse;
import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.model.VideoJobStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.assertj.core.api.Assertions.assertThat;

class JobStatusControllerTest {

    @Test
    void shouldReturnEmptyListWhenNoJobs() {
        Deque<VideoJobResult> history = new ConcurrentLinkedDeque<>();
        JobStatusController controller = new JobStatusController(history);

        ResponseEntity<List<JobStatusResponse>> response = controller.listJobs();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void shouldReturnJobsInHistory() {
        Deque<VideoJobResult> history = new ConcurrentLinkedDeque<>();
        history.add(VideoJobResult.builder(UUID.randomUUID(), "user-1")
                .status(VideoJobStatus.DONE)
                .frameCount(10)
                .outputKey("outputs/user-1/test/frames.zip")
                .build());
        JobStatusController controller = new JobStatusController(history);

        ResponseEntity<List<JobStatusResponse>> response = controller.listJobs();

        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().status()).isEqualTo("DONE");
    }
}
