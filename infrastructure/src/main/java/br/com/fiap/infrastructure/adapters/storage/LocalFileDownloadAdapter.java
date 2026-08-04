package br.com.fiap.infrastructure.adapters.storage;

import br.com.fiap.domain.exceptions.VideoDownloadException;
import br.com.fiap.domain.ports.out.VideoDownloadPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalFileDownloadAdapter implements VideoDownloadPort {

    private static final Logger log = LoggerFactory.getLogger(LocalFileDownloadAdapter.class);

    private final String basePath;

    public LocalFileDownloadAdapter(@Value("${app.storage.local.path:./storage}") String basePath) {
        this.basePath = basePath;
    }

    @Override
    public Path download(String storageKey) {
        try {
            Path source = Paths.get(basePath, storageKey);
            Path tempFile = Files.createTempFile("video-download-", ".tmp");
            Files.copy(source, tempFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("[LOCAL-STORAGE] Downloaded {} to {}", source, tempFile);
            return tempFile;
        } catch (Exception e) {
            throw new VideoDownloadException(storageKey, e);
        }
    }
}
