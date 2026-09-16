package org.challenge.vulnseverityevaluator.presentation.controller.response;

import org.challenge.vulnseverityevaluator.domain.model.MetricChoice;
import org.challenge.vulnseverityevaluator.domain.model.Provenance;
import org.challenge.vulnseverityevaluator.domain.model.SeverityScore;
import org.challenge.vulnseverityevaluator.domain.model.VulnerabilityEvaluation;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The response body, assembled from the aggregate by the presentation layer.
 * <p>
 * It carries the domain types rather than mirroring them. The score, the provenance and each metric choice already
 * have exactly the shape a consumer needs, so the four parallel records that used to restate them field by field
 * bought nothing. What this type adds is the only thing that is genuinely presentation: which parts of the aggregate
 * are exposed, and in what order.
 * <p>
 * The contract avoids the vocabulary of any particular scheme — {@code baseline} and {@code contextual} rather than
 * CVSS terms, with {@code scheme} stated explicitly — so a consumer can read the score and the rating without
 * knowing the scheme, and a change of scheme does not break the contract.
 * <p>
 * {@code advisory} is always true and is not decoration: this output is decision support for a human, never an
 * authorisation to remediate automatically. The provenance block carries the confidence, the review flag and where
 * each input came from, so a consumer can tell when not to rely on the number.
 */
public record SeverityEvaluationResponse(UUID evaluationId,
                                         String vulnerability,
                                         String application,
                                         String scheme,
                                         SeverityScore baseline,
                                         SeverityScore contextual,
                                         BigDecimal delta,
                                         String summary,
                                         List<MetricChoice> justification,
                                         Provenance provenance,
                                         boolean advisory) {

    private static final boolean ADVISORY = true;

    public static SeverityEvaluationResponse createSeverityEvaluationResponse(VulnerabilityEvaluation evaluation) {
        return new SeverityEvaluationResponse(
                evaluation.id(),
                evaluation.vulnerabilityIdentifier(),
                evaluation.applicationContext().name(),
                evaluation.schemeId(),
                evaluation.baseline(),
                evaluation.contextual(),
                evaluation.delta(),
                evaluation.summary(),
                evaluation.justification(),
                evaluation.provenance(),
                ADVISORY);
    }
}
