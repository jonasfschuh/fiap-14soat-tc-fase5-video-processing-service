package br.com.fiap.application.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record JobStatusResponse(
        UUID videoId,
        String userId,
        String status,
        int frameCount,
        String outputKey,
        String errorMessage,
        LocalDateTime processedAt
) {
}
