package org.challenge.vulnseverityevaluator.domain.service;

import org.challenge.vulnseverityevaluator.domain.model.Confidence;
import org.challenge.vulnseverityevaluator.domain.model.MetricChoice;
import org.challenge.vulnseverityevaluator.domain.model.ModelSeverityProposal;
import org.challenge.vulnseverityevaluator.domain.model.SeverityAssessment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.challenge.vulnseverityevaluator.domain.model.BaselineVectorSource.CALLER_SUPPLIED;
import static org.challenge.vulnseverityevaluator.domain.model.BaselineVectorSource.MODEL_DERIVED;
import static org.challenge.vulnseverityevaluator.domain.model.MetricChoice.createMetricChoice;
import org.challenge.vulnseverityevaluator.domain.model.SeverityRating;

import static org.challenge.vulnseverityevaluator.domain.model.SeverityScore.createSeverityScore;

/**
 * When the service says "do not rely on this". Pure rules, so they are tested without a model, a database or a
 * Spring context — which is the point of having taken them out of the aggregate.
 */
class EvaluationPolicyTest {

    private final EvaluationPolicy policy = new EvaluationPolicy();

    @Test
    void givenDerivedBaselineWhenDecidingThenReturnLowConfidence() {
        assertThat(policy.confidence(proposal(0, 4), MODEL_DERIVED)).isEqualTo(Confidence.LOW);
    }

    @Test
    void givenMostlyAbstainedAnswerWhenDecidingThenReturnMediumConfidence() {
        assertThat(policy.confidence(proposal(3, 4), CALLER_SUPPLIED)).isEqualTo(Confidence.MEDIUM);
    }

    @Test
    void givenCommittedAnswerAndSuppliedBaselineWhenDecidingThenReturnHighConfidence() {
        assertThat(policy.confidence(proposal(1, 4), CALLER_SUPPLIED)).isEqualTo(Confidence.HIGH);
    }

    @Test
    void givenSevereOutcomeWhenDecidingThenSetReviewRequired() {
        assertThat(policy.requiresReview(assessment("9.0", "9.0"), CALLER_SUPPLIED)).isTrue();
    }

    @Test
    void givenLargeMovementFromBaselineWhenDecidingThenSetReviewRequired() {
        assertThat(policy.requiresReview(assessment("6.5", "4.0"), CALLER_SUPPLIED)).isTrue();
    }

    @Test
    void givenDerivedBaselineWhenDecidingThenSetReviewRequired() {
        assertThat(policy.requiresReview(assessment("2.0", "2.0"), MODEL_DERIVED)).isTrue();
    }

    @Test
    void givenMildOutcomeAndSuppliedBaselineWhenDecidingThenSetReviewNotRequired() {
        assertThat(policy.requiresReview(assessment("5.0", "4.0"), CALLER_SUPPLIED)).isFalse();
    }

    private static ModelSeverityProposal proposal(int abstentions, int total) {
        List<MetricChoice> choices = IntStream.range(0, total)
                .mapToObj(index -> createMetricChoice("M" + index, index < abstentions ? "X" : "H", "because"))
                .toList();
        return new ModelSeverityProposal(choices, "summary");
    }

    private static SeverityAssessment assessment(String baseline, String contextual) {
        return new SeverityAssessment("CVSS:3.1",
                createSeverityScore(new BigDecimal(baseline), rating(baseline), "vector"),
                createSeverityScore(new BigDecimal(contextual), rating(contextual), "vector"),
                "summary", List.of());
    }

    private static SeverityRating rating(String score) {
        return new BigDecimal(score).compareTo(new BigDecimal("7.0")) >= 0 ? SeverityRating.CRITICAL : SeverityRating.MEDIUM;
    }
}
