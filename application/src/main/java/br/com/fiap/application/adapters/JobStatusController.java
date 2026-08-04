package br.com.fiap.application.adapters;

import br.com.fiap.application.dtos.JobStatusResponse;
import br.com.fiap.domain.model.VideoJobResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Deque;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs", description = "In-memory job history for debug/monitoring purposes")
public class JobStatusController {

    private final Deque<VideoJobResult> jobHistory;

    public JobStatusController(Deque<VideoJobResult> jobHistory) {
        this.jobHistory = jobHistory;
    }

    @GetMapping
    @Operation(
            summary = "List recent processing jobs",
            description = "Returns the last N video processing jobs handled by this instance (in-memory only)."
    )
    public ResponseEntity<List<JobStatusResponse>> listJobs() {
        List<JobStatusResponse> response = jobHistory.stream()
                .map(result -> new JobStatusResponse(
                        result.getVideoId(),
                        result.getUserId(),
                        result.getStatus().name(),
                        result.getFrameCount(),
                        result.getOutputKey(),
                        result.getErrorMessage(),
                        result.getProcessedAt()
                ))
                .toList();
        return ResponseEntity.ok(response);
    }
}
