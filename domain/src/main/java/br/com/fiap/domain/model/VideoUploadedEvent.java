package br.com.fiap.domain.model;

import java.util.UUID;

public record VideoUploadedEvent(
        UUID videoId,
        String userId,
        String storageKey,
        String originalFilename,
        Long fileSizeBytes,
        String mimeType,
        String timestamp
) {
}
