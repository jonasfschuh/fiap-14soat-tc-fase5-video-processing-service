package br.com.fiap.infrastructure.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RestTemplateConfigurationTest {

    @Test
    void shouldCreateRestTemplateBean() {
        RestTemplateConfiguration config = new RestTemplateConfiguration();
        RestTemplate restTemplate = config.restTemplate();
        assertThat(restTemplate).isNotNull();
    }
}
