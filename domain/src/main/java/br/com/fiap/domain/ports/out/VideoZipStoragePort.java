package br.com.fiap.domain.ports.out;

import java.nio.file.Path;

public interface VideoZipStoragePort {
    String store(String outputKey, Path zipPath);
}
