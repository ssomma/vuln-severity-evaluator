package org.challenge.vulnseverityevaluator.configuration;

import org.challenge.vulnseverityevaluator.datasource.llm.SeverityReasoningModel;
import org.challenge.vulnseverityevaluator.domain.service.SpecificationCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the external-model profile has the same scoring catalog as the local profile. */
@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
@ActiveProfiles("production-openai")
class OpenAiProviderProfileTest {

    @Autowired
    private SeverityReasoningModel model;

    @Autowired
    private SpecificationCatalog catalog;

    @Test
    void givenOpenAiProductionProfileWhenStartingThenReturnExternalModelAndSeededCatalog() {
        assertThat(model.identifier()).isEqualTo("openai/gpt-4o-mini");
        assertThat(catalog.specification("CVSS:3.1")).hasSize(11);
    }
}
