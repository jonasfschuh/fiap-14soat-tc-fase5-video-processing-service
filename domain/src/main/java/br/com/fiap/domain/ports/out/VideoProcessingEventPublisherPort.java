package br.com.fiap.domain.ports.out;

import br.com.fiap.domain.model.VideoJobResult;

import java.util.UUID;

public interface VideoProcessingEventPublisherPort {
    void publishProcessed(VideoJobResult result);

    void publishFailed(UUID videoId, String userId, String errorMessage);
}
