package org.challenge.vulnseverityevaluator.domain.model;

import org.challenge.vulnseverityevaluator.domain.service.SpecificationCatalog;
import org.challenge.vulnseverityevaluator.domain.service.impl.Cvss31;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.challenge.vulnseverityevaluator.domain.model.MetricChoice.createMetricChoice;
import static org.challenge.vulnseverityevaluator.domain.model.ModelSeverityProposal.createModelSeverityProposal;

/**
 * The gate between the language model and the domain. Every case here is an answer a real model can plausibly give,
 * and in every case the expected outcome is a rejection rather than a best effort interpretation.
 */
@SpringBootTest
@ActiveProfiles("local")
class ModelSeverityProposalTest {

    private static final String SUMMARY = "because the application is internal";
    private static final String NOT_DEFINED = "X";

    @Autowired
    private SpecificationCatalog catalog;

    @Autowired
    private Cvss31 scheme;

    private List<SchemeMetric> vocabulary;

    @BeforeEach
    void setUp() {
        vocabulary = catalog.specification(scheme.id());
    }

    @Test
    void givenCompleteAnswerWhenCreatingThenReturnProposal() {
        ModelSeverityProposal created = createModelSeverityProposal(complete(), SUMMARY, vocabulary);

        assertThat(created.choices()).hasSameSizeAs(vocabulary);
        assertThat(created.abstentions()).isEqualTo(vocabulary.size());
    }

    @Test
    void givenValueOutsideVocabularyWhenCreatingThenThrowInvalidModelProposalException() {
        List<MetricChoice> choices = complete();
        choices.set(0, createMetricChoice(vocabulary.getFirst().code(), "EXTREME", SUMMARY));

        assertThatThrownBy(() -> createModelSeverityProposal(choices, SUMMARY, vocabulary))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not admitted");
    }

    /**
     * A model that volunteers a number is answering the wrong question. The number is read nowhere, and putting it
     * where a metric value belongs is rejected like any other unadmitted value.
     */
    @Test
    void givenNumericScoreInsteadOfMetricValueWhenCreatingThenThrowInvalidModelProposalException() {
        List<MetricChoice> choices = complete();
        choices.set(0, createMetricChoice(vocabulary.getFirst().code(), "9.8", SUMMARY));

        assertThatThrownBy(() -> createModelSeverityProposal(choices, SUMMARY, vocabulary))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not admitted");
    }

    @Test
    void givenUnknownMetricWhenCreatingThenThrowInvalidModelProposalException() {
        List<MetricChoice> choices = complete();
        choices.add(createMetricChoice("INVENTED", NOT_DEFINED, SUMMARY));

        assertThatThrownBy(() -> createModelSeverityProposal(choices, SUMMARY, vocabulary))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown metric");
    }

    @Test
    void givenMissingMetricWhenCreatingThenThrowInvalidModelProposalException() {
        List<MetricChoice> incomplete = complete().subList(0, 3);

        assertThatThrownBy(() -> createModelSeverityProposal(incomplete, SUMMARY, vocabulary))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("did not answer");
    }

    @Test
    void givenNoChoicesWhenCreatingThenThrowInvalidModelProposalException() {
        assertThatThrownBy(() -> createModelSeverityProposal(List.of(), SUMMARY, vocabulary))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no metric choices");
    }

    @Test
    void givenNullChoicesWhenCreatingThenThrowInvalidModelProposalException() {
        assertThatThrownBy(() -> createModelSeverityProposal(null, SUMMARY, vocabulary))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenBlankSummaryWhenCreatingThenThrowInvalidModelProposalException() {
        assertThatThrownBy(() -> createModelSeverityProposal(complete(), "  ", vocabulary))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no summary");
    }

    @Test
    void givenSummaryAtStorageLimitWhenCreatingThenReturnProposal() {
        assertThat(createModelSeverityProposal(complete(), "s".repeat(VulnerabilityEvaluation.SUMMARY_MAX_LENGTH),
                vocabulary).summary()).hasSize(VulnerabilityEvaluation.SUMMARY_MAX_LENGTH);
    }

    @Test
    void givenSummaryBeyondStorageLimitWhenCreatingThenThrowInvalidModelProposalException() {
        assertThatThrownBy(() -> createModelSeverityProposal(complete(),
                "s".repeat(VulnerabilityEvaluation.SUMMARY_MAX_LENGTH + 1), vocabulary))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("storage limit");
    }

    @Test
    void givenRationaleAtStorageLimitWhenCreatingThenReturnProposal() {
        List<MetricChoice> choices = complete();
        choices.set(0, createMetricChoice(vocabulary.getFirst().code(), NOT_DEFINED,
                "r".repeat(MetricChoice.RATIONALE_MAX_LENGTH)));

        assertThat(createModelSeverityProposal(choices, SUMMARY, vocabulary).choices().getFirst().rationale())
                .hasSize(MetricChoice.RATIONALE_MAX_LENGTH);
    }

    @Test
    void givenRationaleBeyondStorageLimitWhenCreatingThenThrowInvalidModelProposalException() {
        List<MetricChoice> choices = complete();
        choices.set(0, createMetricChoice(vocabulary.getFirst().code(), NOT_DEFINED,
                "r".repeat(MetricChoice.RATIONALE_MAX_LENGTH + 1)));

        assertThatThrownBy(() -> createModelSeverityProposal(choices, SUMMARY, vocabulary))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("storage limit");
    }

    /**
     * Every admitted value of every metric has to be accepted. A vocabulary that advertises a value the validation
     * then rejects would make the model fail for obeying its own instructions.
     */
    @Test
    void givenEveryAdmittedValueWhenCreatingThenDoNotThrowException() {
        vocabulary.forEach(metric -> metric.admitted().forEach(value -> {
            List<MetricChoice> choices = complete();
            choices.removeIf(choice -> choice.metric().equals(metric.code()));
            choices.add(createMetricChoice(metric.code(), value, SUMMARY));
            assertThat(createModelSeverityProposal(choices, SUMMARY, vocabulary).choices()).isNotEmpty();
        }));
    }

    private List<MetricChoice> complete() {
        List<MetricChoice> choices = new ArrayList<>();
        vocabulary.forEach(metric -> choices.add(createMetricChoice(metric.code(), NOT_DEFINED, SUMMARY)));
        return choices;
    }
}
