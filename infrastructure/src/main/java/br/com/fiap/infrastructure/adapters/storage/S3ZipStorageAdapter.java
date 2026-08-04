package br.com.fiap.infrastructure.adapters.storage;

import br.com.fiap.domain.exceptions.VideoZipException;
import br.com.fiap.domain.ports.out.VideoZipStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;

public class S3ZipStorageAdapter implements VideoZipStoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3ZipStorageAdapter.class);

    private final S3Client s3Client;
    private final String bucket;

    public S3ZipStorageAdapter(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public String store(String outputKey, Path zipPath) {
        try {
            long size = Files.size(zipPath);
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(outputKey)
                    .contentType("application/zip")
                    .contentLength(size)
                    .build();
            s3Client.putObject(request, RequestBody.fromFile(zipPath));
            log.info("[S3-STORAGE] ZIP uploaded to s3://{}/{}", bucket, outputKey);
            return outputKey;
        } catch (Exception e) {
            throw new VideoZipException("Failed to upload ZIP to S3: " + outputKey, e);
        }
    }
}
