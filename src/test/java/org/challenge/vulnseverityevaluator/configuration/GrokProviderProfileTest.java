package org.challenge.vulnseverityevaluator.configuration;

import org.challenge.vulnseverityevaluator.datasource.llm.SeverityReasoningModel;
import org.challenge.vulnseverityevaluator.domain.service.SpecificationCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies that Grok uses the same production datasource through its compatible endpoint. */
@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
@ActiveProfiles("production-grok")
class GrokProviderProfileTest {

    @Autowired
    private SeverityReasoningModel model;

    @Autowired
    private SpecificationCatalog catalog;

    @Test
    void givenGrokProductionProfileWhenStartingThenReturnExternalModelAndSeededCatalog() {
        assertThat(model.identifier()).isEqualTo("grok/grok-4");
        assertThat(catalog.specification("CVSS:3.1")).hasSize(11);
    }
}
