package org.challenge.vulnseverityevaluator.domain.service.impl;

import org.challenge.vulnseverityevaluator.domain.model.*;
import org.challenge.vulnseverityevaluator.domain.service.VulnerabilitySeverityService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.challenge.vulnseverityevaluator.domain.model.BaselineVectorSource.CALLER_SUPPLIED;
import static org.challenge.vulnseverityevaluator.domain.model.Confidence.HIGH;
import static org.challenge.vulnseverityevaluator.domain.model.EvaluationConfiguration.createEvaluationConfiguration;
import static org.challenge.vulnseverityevaluator.domain.model.MetricChoice.createMetricChoice;
import static org.challenge.vulnseverityevaluator.domain.model.Provenance.createProvenance;
import static org.challenge.vulnseverityevaluator.domain.model.SeverityRating.CRITICAL;
import static org.challenge.vulnseverityevaluator.domain.model.SeverityScore.createSeverityScore;
import static org.challenge.vulnseverityevaluator.domain.model.VulnerabilityEvaluation.createVulnerabilityEvaluation;

/**
 * A service that returns a canned evaluation, so the HTTP edge can be tested against a real aggregate without the
 * scheme, the catalog or a database behind it.
 * <p>
 * It lives in the layer its production counterpart lives in, mirroring {@code src/main}, and it is wired only from a
 * test configuration — so it can never be picked up by the running application.
 */
public class FakeSeverityService implements VulnerabilitySeverityService {

    private static final String VECTOR = "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H";

    private RuntimeException failure;

    public void failWith(RuntimeException exception) {
        this.failure = exception;
    }

    /**
     * The fake is a singleton bean, so a failure set by one test would leak into the next one and make it fail for
     * the wrong reason. Every test starts from a clean fake.
     */
    public void reset() {
        this.failure = null;
    }

    @Override
    public VulnerabilityEvaluation evaluate(Vulnerability vulnerability, ApplicationContext context) {
        Optional.ofNullable(failure).ifPresent(exception -> {
            throw exception;
        });
        Provenance provenance = createProvenance(
                createEvaluationConfiguration("fake-model", "fake-v1", "catalog-v1", "policy-v1"),
                CALLER_SUPPLIED, HIGH, true);
        return createVulnerabilityEvaluation(vulnerability, context, assessment(), provenance);
    }

    @Override
    public Optional<VulnerabilityEvaluation> find(UUID id) {
        return Optional.empty();
    }

    private static SeverityAssessment assessment() {
        return new SeverityAssessment("CVSS:3.1",
                createSeverityScore(new BigDecimal("10.0"), CRITICAL, VECTOR),
                createSeverityScore(new BigDecimal("9.6"), CRITICAL, VECTOR + "/CR:H"),
                "the application handles personal data and is reachable from the internet",
                List.of(createMetricChoice("CR", "H", "handles personally identifiable information")));
    }
}
