package br.com.fiap.infrastructure.adapters.ffmpeg;

import br.com.fiap.domain.exceptions.FfmpegProcessingException;
import br.com.fiap.domain.ports.out.FfmpegPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class FfmpegAdapter implements FfmpegPort {

    private static final Logger log = LoggerFactory.getLogger(FfmpegAdapter.class);

    private final long timeoutMinutes;

    public FfmpegAdapter(@Value("${app.processing.ffmpeg.timeout-minutes:10}") long timeoutMinutes) {
        this.timeoutMinutes = timeoutMinutes;
    }

    @Override
    public int extractFrames(Path videoPath, Path outputDir) {
        String framePattern = outputDir.resolve("frame_%04d.png").toString();

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", videoPath.toAbsolutePath().toString(),
                "-vf", "fps=1",
                "-y",
                framePattern
        );
        pb.redirectErrorStream(true);

        log.info("[FFMPEG] Starting: ffmpeg -i {} -vf fps=1 -y {}", videoPath, framePattern);

        try {
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }

            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new FfmpegProcessingException("ffmpeg timed out after " + timeoutMinutes + " minutes");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new FfmpegProcessingException("ffmpeg exited with code " + exitCode + ": " + output);
            }

            try (var files = Files.list(outputDir)) {
                long frameCount = files.filter(path -> path.toString().endsWith(".png")).count();
                log.info("[FFMPEG] Extracted {} frames", frameCount);
                return (int) frameCount;
            }
        } catch (FfmpegProcessingException e) {
            throw e;
        } catch (IOException e) {
            throw new FfmpegProcessingException("Failed to launch ffmpeg process", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FfmpegProcessingException("ffmpeg process interrupted", e);
        }
    }
}
