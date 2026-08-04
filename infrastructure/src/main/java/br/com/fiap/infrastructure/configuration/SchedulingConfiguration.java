package br.com.fiap.infrastructure.configuration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "aws.sqs.enabled", havingValue = "true", matchIfMissing = false)
public class SchedulingConfiguration {
}
