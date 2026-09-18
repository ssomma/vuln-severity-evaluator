package org.challenge.vulnseverityevaluator.domain.service.impl;

import org.challenge.vulnseverityevaluator.domain.model.MetricChoice;
import org.challenge.vulnseverityevaluator.domain.model.ModelSeverityProposal;
import org.challenge.vulnseverityevaluator.domain.model.SchemeMetric;
import org.challenge.vulnseverityevaluator.domain.model.SeverityAssessment;
import org.challenge.vulnseverityevaluator.domain.service.SpecificationCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.challenge.vulnseverityevaluator.domain.model.MetricChoice.createMetricChoice;
import static org.challenge.vulnseverityevaluator.domain.model.ModelSeverityProposal.createModelSeverityProposal;
import static org.challenge.vulnseverityevaluator.domain.model.SeverityRating.CRITICAL;
import static org.challenge.vulnseverityevaluator.domain.model.SeverityRating.MEDIUM;
import static org.challenge.vulnseverityevaluator.domain.model.SeverityRating.NONE;

/**
 * The arithmetic, checked against vectors published with the CVSS v3.1 specification.
 * <p>
 * It runs against the seeded catalog rather than against hand built objects, so it validates two things at once: the
 * formulas here and the coefficients in the database. A wrong weight in the seed now fails a golden case instead of
 * quietly shifting every score by a tenth.
 */
@SpringBootTest
@ActiveProfiles("local")
class Cvss31Test {

    private static final String NOT_DEFINED = "X";
    private static final String SUMMARY = "test rationale";

    @Autowired
    private Cvss31 scheme;

    @Autowired
    private SpecificationCatalog catalog;

    private List<SchemeMetric> specification;

    @BeforeEach
    void setUp() {
        specification = catalog.specification(scheme.id());
    }

    @ParameterizedTest
    @CsvSource({
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H, 10.0",
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H, 9.8",
            "CVSS:3.1/AV:L/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:H, 7.8",
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N, 7.5",
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:H, 7.5",
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:R/S:C/C:L/I:L/A:N, 6.1",
            "CVSS:3.1/AV:P/AC:H/PR:H/UI:R/S:U/C:L/I:N/A:N, 1.6"
    })
    void givenOfficialVectorWhenAssessingThenReturnSpecifiedBaselineScore(String vector, String expected) {
        assertThat(assess(vector).baseline().score()).isEqualByComparingTo(new BigDecimal(expected));
    }

    @Test
    void givenVectorWithoutImpactWhenAssessingThenReturnZeroAndRatingNone() {
        SeverityAssessment assessment = assess("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N");

        assertThat(assessment.baseline().score()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(assessment.baseline().rating()).isEqualTo(NONE);
    }

    @Test
    void givenNoContextualEvidenceWhenAssessingThenReturnContextualScoreEqualToBaseline() {
        SeverityAssessment assessment = assess("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");

        assertThat(assessment.contextual().score()).isEqualByComparingTo(assessment.baseline().score());
        assertThat(assessment.delta()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void givenLowCriticalityInternalApplicationWhenAssessingThenReturnLoweredContextualScore() {
        SeverityAssessment assessment = assess("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
                "CR", "L", "IR", "L", "AR", "L", "AV", "L");

        assertThat(assessment.baseline().score()).isEqualByComparingTo(new BigDecimal("9.8"));
        assertThat(assessment.contextual().score()).isEqualByComparingTo(new BigDecimal("6.6"));
        assertThat(assessment.contextual().rating()).isEqualTo(MEDIUM);
        assertThat(assessment.delta()).isNegative();
    }

    @Test
    void givenHighCriticalityApplicationWhenAssessingThenReturnRaisedContextualScore() {
        SeverityAssessment assessment = assess("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N",
                "CR", "H", "IR", "H", "AR", "H");

        assertThat(assessment.contextual().score()).isGreaterThan(assessment.baseline().score());
        assertThat(assessment.delta()).isPositive();
    }

    @Test
    void givenAssessmentWhenReadingContextualVectorThenReturnVectorWithContextualMetrics() {
        SeverityAssessment assessment = assess("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H",
                "CR", "H", "IR", "M", "AR", "L");

        assertThat(assessment.baseline().vector()).isEqualTo("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H");
        assertThat(assessment.contextual().vector()).contains("CR:H", "IR:M", "AR:L", "MAV:X");
        assertThat(assessment.schemeId()).isEqualTo("CVSS:3.1");
    }

    /**
     * Every metric has to reach the justification. A contextualisation that silently dropped one would score with a
     * value nobody can see afterwards.
     */
    @Test
    void givenAssessmentWhenReadingJustificationThenReturnOneEntryPerMetric() {
        SeverityAssessment assessment = assess("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");

        assertThat(assessment.justification()).hasSameSizeAs(specification);
    }

    @Test
    void givenLog4ShellVectorWhenAssessingThenReturnCriticalRating() {
        assertThat(assess("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H").baseline().rating()).isEqualTo(CRITICAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "   ", "not-a-vector",
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H",
            "CVSS:3.1/AV:Z/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
            "CVSS:3.1/AV:/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
            "CVSS:4.0/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
            "AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H",
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/",
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H/E:P",
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/AV:L",
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/ZZ:H",
            "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H:extra"
    })
    void givenMalformedVectorWhenAssessingThenThrowIllegalArgumentException(String vector) {
        assertThatThrownBy(() -> assess(vector)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenNullVectorWhenAssessingThenThrowIllegalArgumentException() {
        assertThatThrownBy(() -> assess(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void givenOutOfOrderBaseMetricsWhenAssessingThenReturnCanonicalVector() {
        SeverityAssessment assessment = assess("CVSS:3.1/A:H/I:H/C:H/S:U/UI:N/PR:N/AC:L/AV:N");

        assertThat(assessment.baseline().vector())
                .isEqualTo("CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H");
    }

    private SeverityAssessment assess(String vector, String... overrides) {
        return scheme.assess(vector, proposal(overrides), specification);
    }

    private ModelSeverityProposal proposal(String... overrides) {
        Map<String, String> chosen = new LinkedHashMap<>();
        IntStream.iterate(0, index -> index < overrides.length, index -> index + 2)
                .forEach(index -> chosen.put(overrides[index], overrides[index + 1]));
        List<MetricChoice> choices = new ArrayList<>();
        specification.forEach(metric -> choices.add(createMetricChoice(
                metric.code(), chosen.getOrDefault(metric.code(), NOT_DEFINED), SUMMARY)));
        return createModelSeverityProposal(choices, SUMMARY, specification);
    }
}
