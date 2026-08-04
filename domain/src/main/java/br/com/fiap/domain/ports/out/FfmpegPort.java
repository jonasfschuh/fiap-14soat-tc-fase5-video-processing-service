package br.com.fiap.domain.ports.out;

import java.nio.file.Path;

public interface FfmpegPort {
    int extractFrames(Path videoPath, Path outputDir);
}
