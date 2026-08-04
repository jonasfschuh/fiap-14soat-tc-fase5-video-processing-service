package br.com.fiap.application.bdd;

import br.com.fiap.application.VideoProcessingApplication;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest(classes = VideoProcessingApplication.class)
public class CucumberSpringConfiguration {
}
