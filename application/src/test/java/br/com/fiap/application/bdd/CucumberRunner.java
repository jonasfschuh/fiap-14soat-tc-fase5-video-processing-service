package br.com.fiap.application.bdd;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "br.com.fiap.application.bdd",
        plugin = {
                "pretty",
                "html:target/cucumber-reports/report.html",
                "junit:target/cucumber-reports/cucumber.xml"
        }
)
public class CucumberRunner {
}
