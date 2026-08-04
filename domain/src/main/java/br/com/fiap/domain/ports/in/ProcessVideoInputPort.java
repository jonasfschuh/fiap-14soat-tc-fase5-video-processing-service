package br.com.fiap.domain.ports.in;

import br.com.fiap.domain.model.VideoJobResult;
import br.com.fiap.domain.model.VideoUploadedEvent;

public interface ProcessVideoInputPort {
    VideoJobResult execute(VideoUploadedEvent event);
}
