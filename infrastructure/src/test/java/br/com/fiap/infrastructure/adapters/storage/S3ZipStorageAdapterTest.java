package br.com.fiap.infrastructure.adapters.storage;

import br.com.fiap.domain.exceptions.VideoZipException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3ZipStorageAdapterTest {

    @Test
    void shouldUploadZipToS3(@TempDir Path tempDir) throws IOException {
        S3Client s3Client = mock(S3Client.class);
        when(s3Client.putObject(org.mockito.ArgumentMatchers.any(PutObjectRequest.class),
                org.mockito.ArgumentMatchers.any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("etag").build());

        Path zipFile = tempDir.resolve("frames.zip");
        Files.writeString(zipFile, "zip-content");

        S3ZipStorageAdapter adapter = new S3ZipStorageAdapter(s3Client, "bucket-a");
        String key = adapter.store("outputs/user/video/frames.zip", zipFile);

        assertThat(key).isEqualTo("outputs/user/video/frames.zip");
        verify(s3Client).putObject(argThat((PutObjectRequest request) ->
                request.bucket().equals("bucket-a")
                        && request.key().equals("outputs/user/video/frames.zip")
                        && request.contentType().equals("application/zip")), any(RequestBody.class));
    }

    @Test
    void shouldWrapUploadFailures(@TempDir Path tempDir) throws IOException {
        S3Client s3Client = mock(S3Client.class);
        doThrow(new RuntimeException("upload failed"))
                .when(s3Client).putObject(org.mockito.ArgumentMatchers.any(PutObjectRequest.class),
                        org.mockito.ArgumentMatchers.any(RequestBody.class));

        Path zipFile = tempDir.resolve("frames.zip");
        Files.writeString(zipFile, "zip-content");

        S3ZipStorageAdapter adapter = new S3ZipStorageAdapter(s3Client, "bucket-a");

        assertThatThrownBy(() -> adapter.store("outputs/user/video/frames.zip", zipFile))
                .isInstanceOf(VideoZipException.class);
    }
}
