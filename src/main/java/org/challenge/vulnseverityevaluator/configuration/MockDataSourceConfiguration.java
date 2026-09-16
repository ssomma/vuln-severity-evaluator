package org.challenge.vulnseverityevaluator.configuration;

import org.challenge.vulnseverityevaluator.datasource.llm.SeverityReasoningModel;
import org.challenge.vulnseverityevaluator.datasource.llm.StubSeverityReasoningModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Wires the deterministic model for the {@code local} profile.
 * <p>
 * With this active the whole flow runs with no API key and no network, so the design can be exercised and reviewed
 * reproducibly. The rest of the application cannot tell the difference: it only ever sees the interface.
 */
@Configuration
@Profile("local")
public class MockDataSourceConfiguration {

    @Bean
    public SeverityReasoningModel severityReasoningModel() {
        return new StubSeverityReasoningModel();
    }
}
