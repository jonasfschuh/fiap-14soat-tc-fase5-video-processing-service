package br.com.fiap.application.bdd.steps;

import br.com.fiap.application.adapters.JobStatusController;
import br.com.fiap.application.dtos.JobStatusResponse;
import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.model.VideoJobStatus;
import br.com.fiap.domain.model.VideoUploadedEvent;
import br.com.fiap.domain.ports.out.FfmpegPort;
import br.com.fiap.domain.ports.out.VideoDownloadPort;
import br.com.fiap.domain.ports.out.VideoProcessingEventPublisherPort;
import br.com.fiap.domain.ports.out.VideoZipStoragePort;
import br.com.fiap.domain.usecases.ProcessVideoUseCase;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class VideoProcessingSteps {

    private VideoUploadedEvent event;
    private boolean ffmpegShouldFail;
    private VideoJobResult result;
    private TestPublisher publisher;
    private ResponseEntity<List<JobStatusResponse>> response;
    private Deque<VideoJobResult> jobHistory;

    @Given("a video-uploaded event with videoId {string} and userId {string}")
    public void aVideoUploadedEventWithVideoIdAndUserId(String videoId, String userId) {
        event = new VideoUploadedEvent(
                UUID.fromString(videoId),
                userId,
                "videos/" + userId + "/" + videoId + "/video.mp4",
                "video.mp4",
                1024L,
                "video/mp4",
                "2025-01-01T10:00:00Z"
        );
        ffmpegShouldFail = false;
        publisher = new TestPublisher();
    }

    @And("the ffmpeg process will fail")
    public void theFfmpegProcessWillFail() {
        ffmpegShouldFail = true;
    }

    @When("the processing service handles the event")
    public void theProcessingServiceHandlesTheEvent() {
        VideoDownloadPort downloadPort = storageKey -> {
            try {
                Path dir = Path.of("target", "bdd-downloads");
                Files.createDirectories(dir);
                Path video = dir.resolve(event.videoId() + ".mp4");
                Files.writeString(video, "fake-video");
                return video;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        FfmpegPort ffmpegPort = (videoPath, outputDir) -> {
            if (ffmpegShouldFail) {
                throw new RuntimeException("simulated ffmpeg failure");
            }
            try {
                Files.writeString(outputDir.resolve("frame_0001.png"), "frame-1");
                Files.writeString(outputDir.resolve("frame_0002.png"), "frame-2");
                return 2;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        VideoZipStoragePort zipStoragePort = (outputKey, zipPath) -> outputKey;

        result = new ProcessVideoUseCase(downloadPort, ffmpegPort, zipStoragePort, publisher).execute(event);
    }

    @Then("the result status should be {string}")
    public void theResultStatusShouldBe(String status) {
        assertThat(result.getStatus().name()).isEqualTo(status);
    }

    @And("the frameCount should be greater than {int}")
    public void theFrameCountShouldBeGreaterThan(int count) {
        assertThat(result.getFrameCount()).isGreaterThan(count);
    }

    @And("a VIDEO_PROCESSED event should be published")
    public void aVIDEO_PROCESSEDEventShouldBePublished() {
        assertThat(publisher.processedPublished.get()).isTrue();
    }

    @And("a VIDEO_FAILED event should be published")
    public void aVIDEO_FAILEDEventShouldBePublished() {
        assertThat(publisher.failedPublished.get()).isTrue();
    }

    @And("no VIDEO_PROCESSED event should be published")
    public void noVIDEO_PROCESSEDEventShouldBePublished() {
        assertThat(publisher.processedPublished.get()).isFalse();
    }

    @Given("the processing service has completed some jobs")
    public void theProcessingServiceHasCompletedSomeJobs() {
        jobHistory = new ConcurrentLinkedDeque<>();
        jobHistory.add(VideoJobResult.builder(UUID.randomUUID(), "user-a")
                .status(VideoJobStatus.DONE)
                .frameCount(4)
                .outputKey("outputs/user-a/job-a/frames.zip")
                .processedAt(LocalDateTime.now())
                .build());
        jobHistory.add(VideoJobResult.builder(UUID.randomUUID(), "user-b")
                .status(VideoJobStatus.FAILED)
                .errorMessage("failure")
                .processedAt(LocalDateTime.now())
                .build());
    }

    @When("a request is made to GET \\/api\\/jobs")
    public void aRequestIsMadeToGETApiJobs() {
        response = new JobStatusController(jobHistory).listJobs();
    }

    @Then("the response should contain the job list")
    public void theResponseShouldContainTheJobList() {
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
    }

    @And("the response status should be {int}")
    public void theResponseStatusShouldBe(int status) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
    }

    private static final class TestPublisher implements VideoProcessingEventPublisherPort {
        private final AtomicBoolean processedPublished = new AtomicBoolean(false);
        private final AtomicBoolean failedPublished = new AtomicBoolean(false);

        @Override
        public void publishProcessed(VideoJobResult result) {
            processedPublished.set(true);
        }

        @Override
        public void publishFailed(UUID videoId, String userId, String errorMessage) {
            failedPublished.set(true);
        }
    }
}
