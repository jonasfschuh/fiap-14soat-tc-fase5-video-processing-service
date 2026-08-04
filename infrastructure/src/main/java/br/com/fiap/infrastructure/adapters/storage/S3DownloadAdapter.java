package br.com.fiap.infrastructure.adapters.storage;

import br.com.fiap.domain.exceptions.VideoDownloadException;
import br.com.fiap.domain.ports.out.VideoDownloadPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;

public class S3DownloadAdapter implements VideoDownloadPort {

    private static final Logger log = LoggerFactory.getLogger(S3DownloadAdapter.class);

    private final S3Client s3Client;
    private final String bucket;

    public S3DownloadAdapter(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public Path download(String storageKey) {
        try {
            Path tempFile = Files.createTempFile("video-s3-", ".tmp");
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build();
            s3Client.getObject(request, tempFile);
            log.info("[S3-STORAGE] Downloaded s3://{}/{} to {}", bucket, storageKey, tempFile);
            return tempFile;
        } catch (Exception e) {
            throw new VideoDownloadException(storageKey, e);
        }
    }
}
