package br.com.fiap.infrastructure.adapters.storage;

import br.com.fiap.domain.exceptions.VideoDownloadException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class S3DownloadAdapterTest {

    @Test
    void shouldDownloadFileFromS3() throws Exception {
        S3Client s3Client = mock(S3Client.class);
        doAnswer(invocation -> {
            Path destination = invocation.getArgument(1);
            Files.writeString(destination, "video-content");
            return GetObjectResponse.builder().build();
        }).when(s3Client).getObject(org.mockito.ArgumentMatchers.any(GetObjectRequest.class),
                org.mockito.ArgumentMatchers.any(Path.class));

        S3DownloadAdapter adapter = new S3DownloadAdapter(s3Client, "bucket-a");

        Path downloaded = adapter.download("videos/user/video.mp4");

        assertThat(downloaded).exists();
        assertThat(Files.readString(downloaded)).isEqualTo("video-content");
        verify(s3Client).getObject(argThat((GetObjectRequest request) ->
                request.bucket().equals("bucket-a") && request.key().equals("videos/user/video.mp4")), any(Path.class));
    }

    @Test
    void shouldWrapErrorsAsVideoDownloadException() {
        S3Client s3Client = mock(S3Client.class);
        doThrow(new RuntimeException("S3 unavailable"))
                .when(s3Client).getObject(org.mockito.ArgumentMatchers.any(GetObjectRequest.class),
                        org.mockito.ArgumentMatchers.any(Path.class));

        S3DownloadAdapter adapter = new S3DownloadAdapter(s3Client, "bucket-a");

        assertThatThrownBy(() -> adapter.download("videos/user/video.mp4"))
                .isInstanceOf(VideoDownloadException.class);
    }
}
