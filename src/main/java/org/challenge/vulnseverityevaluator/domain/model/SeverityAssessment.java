package org.challenge.vulnseverityevaluator.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Outcome of an assessment: the baseline score, the score after applying the declared context, and the reasoning
 * behind every contextual metric the model chose.
 * <p>
 * Both scores are reported, never just the contextual one. The delta is the actual product of this service, and
 * hiding the baseline would make the contextualisation impossible to challenge.
 */
public record SeverityAssessment(String schemeId,
                                 SeverityScore baseline,
                                 SeverityScore contextual,
                                 String summary,
                                 List<MetricChoice> justification) {

    public BigDecimal delta() {
        return contextual.score().subtract(baseline.score());
    }
}
