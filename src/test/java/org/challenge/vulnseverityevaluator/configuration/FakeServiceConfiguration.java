package org.challenge.vulnseverityevaluator.configuration;

import org.challenge.vulnseverityevaluator.domain.service.impl.FakeSeverityService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Wiring for slice tests of the HTTP edge, mirroring how {@code MockDataSourceConfiguration} wires the deterministic
 * model in the {@code local} profile: the fake lives in its own layer, and the decision to use it lives here.
 * <p>
 * A {@code @TestConfiguration} is never picked up by component scanning, so this can only take effect where a test
 * imports it explicitly.
 */
@TestConfiguration
public class FakeServiceConfiguration {

    @Bean
    @Primary
    public FakeSeverityService fakeSeverityService() {
        return new FakeSeverityService();
    }
}
