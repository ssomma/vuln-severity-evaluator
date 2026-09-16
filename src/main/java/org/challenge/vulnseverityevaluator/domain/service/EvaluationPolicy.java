package org.challenge.vulnseverityevaluator.domain.service;

import org.challenge.vulnseverityevaluator.domain.model.BaselineVectorSource;
import org.challenge.vulnseverityevaluator.domain.model.Confidence;
import org.challenge.vulnseverityevaluator.domain.model.ModelSeverityProposal;
import org.challenge.vulnseverityevaluator.domain.model.SeverityAssessment;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static org.challenge.vulnseverityevaluator.domain.model.BaselineVectorSource.MODEL_DERIVED;
import static org.challenge.vulnseverityevaluator.domain.model.SeverityRating.HIGH;

/**
 * Decides how far an evaluation can be trusted and whether a human has to review it.
 * <p>
 * A {@code @Component} rather than a {@code @Service}: presentation never calls it, it is collaboration for the use
 * case, and it touches no datasource. It is separate from the aggregate because these are the rules most likely to be
 * argued about and retuned — thresholds, what counts as abstaining — and they should be changeable without editing
 * the record that stores their outcome.
 * <p>
 * This is the human in the loop expressed in code. Nothing here authorises an automatic remediation, and the cases
 * most likely to be wrong are precisely the ones it forces someone to look at.
 */
@Component
public class EvaluationPolicy {

    private static final BigDecimal SIGNIFICANT_DELTA = new BigDecimal("2.0");

    /**
     * Confidence degrades for the two reasons that actually matter: the anchor was inferred instead of supplied, or
     * the model abstained on most metrics because the declared context carried little evidence.
     */
    public Confidence confidence(ModelSeverityProposal proposal, BaselineVectorSource source) {
        if (source == MODEL_DERIVED) {
            return Confidence.LOW;
        }
        if (proposal.abstentions() * 2 > proposal.choices().size()) {
            return Confidence.MEDIUM;
        }
        return Confidence.HIGH;
    }

    /**
     * Review is required when the outcome is severe, when the baseline itself came from the model, or when the
     * declared context moved the score far from its baseline — the three situations where being wrong costs most.
     */
    public boolean requiresReview(SeverityAssessment assessment, BaselineVectorSource source) {
        boolean severe = assessment.contextual().rating().compareTo(HIGH) >= 0;
        boolean moved = assessment.delta().abs().compareTo(SIGNIFICANT_DELTA) >= 0;
        return severe || moved || source == MODEL_DERIVED;
    }
}
