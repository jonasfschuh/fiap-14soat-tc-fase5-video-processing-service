package br.com.fiap.infrastructure.configuration;

import br.com.fiap.domain.ports.out.FfmpegPort;
import br.com.fiap.domain.ports.out.VideoDownloadPort;
import br.com.fiap.domain.ports.out.VideoProcessingEventPublisherPort;
import br.com.fiap.domain.ports.out.VideoZipStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ProcessingBeanConfigurationTest {

    @Mock
    private VideoDownloadPort downloadPort;
    @Mock
    private FfmpegPort ffmpegPort;
    @Mock
    private VideoZipStoragePort zipStoragePort;
    @Mock
    private VideoProcessingEventPublisherPort eventPublisher;

    @Test
    void shouldCreateJobHistory() {
        ProcessingBeanConfiguration config = new ProcessingBeanConfiguration();
        assertThat(config.jobHistory()).isNotNull().isEmpty();
    }

    @Test
    void shouldCreateFfmpegPort() {
        ProcessingBeanConfiguration config = new ProcessingBeanConfiguration();
        FfmpegPort port = config.ffmpegPort();
        assertThat(port).isNotNull();
    }

    @Test
    void shouldCreateProcessVideoInputPort() {
        ProcessingBeanConfiguration config = new ProcessingBeanConfiguration();
        var port = config.processVideoInputPort(downloadPort, ffmpegPort, zipStoragePort, eventPublisher);
        assertThat(port).isNotNull();
    }
}

