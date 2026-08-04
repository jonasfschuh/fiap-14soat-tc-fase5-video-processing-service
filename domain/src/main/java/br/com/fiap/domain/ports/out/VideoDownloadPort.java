package br.com.fiap.domain.ports.out;

import java.nio.file.Path;

public interface VideoDownloadPort {
    Path download(String storageKey);
}
