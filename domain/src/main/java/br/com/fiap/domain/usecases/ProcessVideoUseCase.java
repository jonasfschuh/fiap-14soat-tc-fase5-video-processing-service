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
import org.slf4j.MDC;

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
        String filename = event.originalFilename();
        Path tempDir = null;
        Path videoFile = null;
        long jobStart = System.currentTimeMillis();

        log.info("Iniciando processamento de \"{}\" | userId: {} | videoId: {}",
                filename, userId, videoId);

        try {
            tempDir = Files.createTempDirectory("video-processing-" + videoId);

            // ── Download ────────────────────────────────────────────────
            MDC.put("processingStage", "DOWNLOADING");
            long t0 = System.currentTimeMillis();
            log.info("[1/4] Baixando \"{}\" do storage — chave: {}", filename, event.storageKey());
            videoFile = downloadPort.download(event.storageKey());
            log.info("[1/4] Download concluído — \"{}\" ({})", filename, formatDuration(elapsed(t0)));

            // ── Frame extraction ────────────────────────────────────────
            MDC.put("processingStage", "EXTRACTING_FRAMES");
            t0 = System.currentTimeMillis();
            log.info("[2/4] Extraindo frames de \"{}\" via FFmpeg...", filename);
            Path framesDir = tempDir.resolve("frames");
            Files.createDirectories(framesDir);
            int frameCount = ffmpegPort.extractFrames(videoFile, framesDir);
            log.info("[2/4] Extração concluída — {} frames de \"{}\" ({})",
                    frameCount, filename, formatDuration(elapsed(t0)));

            // ── ZIP ─────────────────────────────────────────────────────
            MDC.put("processingStage", "COMPRESSING");
            t0 = System.currentTimeMillis();
            log.info("[3/4] Compactando {} frames de \"{}\" em ZIP...", frameCount, filename);
            Path zipFile = tempDir.resolve("frames_" + videoId + ".zip");
            createZip(framesDir, zipFile);
            log.info("[3/4] ZIP criado com sucesso — \"{}\" ({})", filename, formatDuration(elapsed(t0)));

            // ── Store ───────────────────────────────────────────────────
            MDC.put("processingStage", "STORING");
            t0 = System.currentTimeMillis();
            String outputKey = "outputs/" + userId + "/" + videoId + "/frames.zip";
            log.info("[4/4] Armazenando artefato de \"{}\" — destino: {}", filename, outputKey);
            String storedKey = zipStoragePort.store(outputKey, zipFile);
            log.info("[4/4] Artefato armazenado — {} ({})", storedKey, formatDuration(elapsed(t0)));

            // ── Publish ─────────────────────────────────────────────────
            MDC.put("processingStage", "PUBLISHING");
            VideoJobResult result = VideoJobResult.builder(videoId, userId)
                    .outputKey(storedKey)
                    .frameCount(frameCount)
                    .status(VideoJobStatus.DONE)
                    .originalFilename(filename)
                    .fileSizeBytes(event.fileSizeBytes())
                    .mimeType(event.mimeType())
                    .storageKey(event.storageKey())
                    .build();
            eventPublisher.publishProcessed(result);

            log.info("<<< \"{}\" processado com SUCESSO — frames: {} | duração total: {}",
                    filename, frameCount, formatDuration(elapsed(jobStart)));
            return result;

        } catch (Exception ex) {
            log.error("<<< FALHA ao processar \"{}\" — erro: {} | duração: {}",
                    filename, ex.getMessage(), formatDuration(elapsed(jobStart)), ex);
            eventPublisher.publishFailed(videoId, userId, ex.getMessage());
            return VideoJobResult.builder(videoId, userId)
                    .status(VideoJobStatus.FAILED)
                    .errorMessage(ex.getMessage())
                    .build();
        } finally {
            MDC.remove("processingStage");
            cleanupPath(videoFile);
            cleanupDirectory(tempDir);
        }
    }

    private static long elapsed(long since) {
        return System.currentTimeMillis() - since;
    }

    private static String formatDuration(long ms) {
        return ms < 1000 ? ms + "ms" : String.format("%.1fs", ms / 1000.0);
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
