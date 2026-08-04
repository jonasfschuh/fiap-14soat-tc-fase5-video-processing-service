package br.com.fiap.infrastructure.adapters.storage;

import br.com.fiap.domain.exceptions.VideoZipException;
import br.com.fiap.domain.ports.out.VideoZipStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalFileZipStorageAdapter implements VideoZipStoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalFileZipStorageAdapter.class);

    private final String outputPath;

    public LocalFileZipStorageAdapter(@Value("${app.storage.local.output-path:./outputs}") String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public String store(String outputKey, Path zipPath) {
        try {
            Path target = Paths.get(outputPath, outputKey);
            Files.createDirectories(target.getParent());
            Files.copy(zipPath, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("[LOCAL-STORAGE] ZIP stored at {}", target.toAbsolutePath());
            return outputKey;
        } catch (Exception e) {
            throw new VideoZipException("Failed to store ZIP locally: " + outputKey, e);
        }
    }
}
