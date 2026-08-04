package br.com.fiap.domain.usecases;

import br.com.fiap.domain.exceptions.VideoZipException;
import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.model.VideoJobStatus;
import br.com.fiap.domain.model.VideoUploadedEvent;
import br.com.fiap.domain.ports.in.ProcessVideoInputPort;
import br.com.fiap.domain.ports.out.FfmpegPort;
import br.com.fiap.domain.ports.out.VideoDownloadPort;
import br.com.fiap.domain.ports.out.VideoProcessingEventPublisherPort;
import br.com.fiap.domain.ports.out.VideoZipStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ProcessVideoUseCase implements ProcessVideoInputPort {

    private static final Logger log = LoggerFactory.getLogger(ProcessVideoUseCase.class);

    private final VideoDownloadPort downloadPort;
    private final FfmpegPort ffmpegPort;
    private final VideoZipStoragePort zipStoragePort;
    private final VideoProcessingEventPublisherPort eventPublisher;

    public ProcessVideoUseCase(VideoDownloadPort downloadPort,
                               FfmpegPort ffmpegPort,
                               VideoZipStoragePort zipStoragePort,
                               VideoProcessingEventPublisherPort eventPublisher) {
        this.downloadPort = downloadPort;
        this.ffmpegPort = ffmpegPort;
        this.zipStoragePort = zipStoragePort;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public VideoJobResult execute(VideoUploadedEvent event) {
        UUID videoId = event.videoId();
        String userId = event.userId();
        Path tempDir = null;
        Path videoFile = null;

        log.info("[PROCESSING] Starting job for videoId={} userId={}", videoId, userId);

        try {
            tempDir = Files.createTempDirectory("video-processing-" + videoId);

            videoFile = downloadPort.download(event.storageKey());
            log.info("[PROCESSING] Downloaded video to {}", videoFile);

            Path framesDir = tempDir.resolve("frames");
            Files.createDirectories(framesDir);
            int frameCount = ffmpegPort.extractFrames(videoFile, framesDir);
            log.info("[PROCESSING] Extracted {} frames for videoId={}", frameCount, videoId);

            Path zipFile = tempDir.resolve("frames_" + videoId + ".zip");
            createZip(framesDir, zipFile);

            String outputKey = "outputs/" + userId + "/" + videoId + "/frames.zip";
            String storedKey = zipStoragePort.store(outputKey, zipFile);
            log.info("[PROCESSING] ZIP stored at {}", storedKey);

            VideoJobResult result = VideoJobResult.builder(videoId, userId)
                    .outputKey(storedKey)
                    .frameCount(frameCount)
                    .status(VideoJobStatus.DONE)
                    .build();

            eventPublisher.publishProcessed(result);
            log.info("[PROCESSING] Completed videoId={} frames={}", videoId, frameCount);
            return result;
        } catch (Exception ex) {
            log.error("[PROCESSING] Failed videoId={} error={}", videoId, ex.getMessage(), ex);
            eventPublisher.publishFailed(videoId, userId, ex.getMessage());
            return VideoJobResult.builder(videoId, userId)
                    .status(VideoJobStatus.FAILED)
                    .errorMessage(ex.getMessage())
                    .build();
        } finally {
            cleanupPath(videoFile);
            cleanupDirectory(tempDir);
        }
    }

    private void createZip(Path framesDir, Path zipFile) {
        try (OutputStream fos = Files.newOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos);
             var frames = Files.list(framesDir).filter(path -> path.toString().endsWith(".png")).sorted()) {

            for (Path frame : (Iterable<Path>) frames::iterator) {
                ZipEntry entry = new ZipEntry(frame.getFileName().toString());
                zos.putNextEntry(entry);
                Files.copy(frame, zos);
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new VideoZipException("Failed to create ZIP from extracted frames", e);
        }
    }

    private void cleanupPath(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("[PROCESSING] Failed to delete temp file {}: {}", path, ex.getMessage());
        }
    }

    private void cleanupDirectory(Path tempDir) {
        if (tempDir == null) {
            return;
        }
        try (var walk = Files.walk(tempDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    log.debug("[PROCESSING] Ignored cleanup failure for {}", path);
                }
            });
        } catch (IOException ex) {
            log.warn("[PROCESSING] Failed to clean temp dir {}: {}", tempDir, ex.getMessage());
        }
    }
}
