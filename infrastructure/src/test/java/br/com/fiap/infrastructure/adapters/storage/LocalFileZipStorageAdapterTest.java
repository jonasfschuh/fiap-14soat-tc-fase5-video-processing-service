package br.com.fiap.infrastructure.adapters.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileZipStorageAdapterTest {

    @Test
    void shouldStoreZipAndReturnKey(@TempDir Path tempDir) throws IOException {
        Path zipFile = tempDir.resolve("frames.zip");
        Files.writeString(zipFile, "fake-zip-content");

        Path outputDir = tempDir.resolve("outputs");
        LocalFileZipStorageAdapter adapter = new LocalFileZipStorageAdapter(outputDir.toString());

        String key = adapter.store("outputs/user/uuid/frames.zip", zipFile);

        assertThat(key).isEqualTo("outputs/user/uuid/frames.zip");
        assertThat(outputDir.resolve("outputs/user/uuid/frames.zip")).exists();
    }
}
