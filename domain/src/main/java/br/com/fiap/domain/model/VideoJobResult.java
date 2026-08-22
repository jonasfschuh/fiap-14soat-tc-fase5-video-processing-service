package br.com.fiap.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class VideoJobResult {

    private final UUID videoId;
    private final String userId;
    private final String outputKey;
    private final int frameCount;
    private final VideoJobStatus status;
    private final String errorMessage;
    private final LocalDateTime processedAt;
    private final String originalFilename;
    private final Long fileSizeBytes;
    private final String mimeType;
    private final String storageKey;

    private VideoJobResult(Builder builder) {
        this.videoId = builder.videoId;
        this.userId = builder.userId;
        this.outputKey = builder.outputKey;
        this.frameCount = builder.frameCount;
        this.status = builder.status;
        this.errorMessage = builder.errorMessage;
        this.processedAt = builder.processedAt != null ? builder.processedAt : LocalDateTime.now();
        this.originalFilename = builder.originalFilename;
        this.fileSizeBytes = builder.fileSizeBytes;
        this.mimeType = builder.mimeType;
        this.storageKey = builder.storageKey;
    }

    public static Builder builder(UUID videoId, String userId) {
        return new Builder(videoId, userId);
    }

    public UUID getVideoId() {
        return videoId;
    }

    public String getUserId() {
        return userId;
    }

    public String getOutputKey() {
        return outputKey;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public VideoJobStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public static class Builder {
        private final UUID videoId;
        private final String userId;
        private String outputKey;
        private int frameCount;
        private VideoJobStatus status = VideoJobStatus.PROCESSING;
        private String errorMessage;
        private LocalDateTime processedAt;
        private String originalFilename;
        private Long fileSizeBytes;
        private String mimeType;
        private String storageKey;

        public Builder(UUID videoId, String userId) {
            this.videoId = videoId;
            this.userId = userId;
        }

        public Builder outputKey(String outputKey) {
            this.outputKey = outputKey;
            return this;
        }

        public Builder frameCount(int frameCount) {
            this.frameCount = frameCount;
            return this;
        }

        public Builder status(VideoJobStatus status) {
            this.status = status;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder processedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
            return this;
        }

        public Builder originalFilename(String originalFilename) {
            this.originalFilename = originalFilename;
            return this;
        }

        public Builder fileSizeBytes(Long fileSizeBytes) {
            this.fileSizeBytes = fileSizeBytes;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Builder storageKey(String storageKey) {
            this.storageKey = storageKey;
            return this;
        }

        public VideoJobResult build() {
            return new VideoJobResult(this);
        }
    }
}
