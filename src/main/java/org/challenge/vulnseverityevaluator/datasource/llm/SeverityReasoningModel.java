package org.challenge.vulnseverityevaluator.datasource.llm;

import org.challenge.vulnseverityevaluator.domain.model.ContextAttribute;
import org.challenge.vulnseverityevaluator.domain.model.ModelSeverityProposal;
import org.challenge.vulnseverityevaluator.domain.model.SchemeMetric;
import org.challenge.vulnseverityevaluator.domain.model.Vulnerability;

import java.util.List;

/**
 * A language model treated as a datasource: it is asked for an opinion and it answers with data.
 * <p>
 * It is not a decision engine. It proposes a value for every metric of the vocabulary it is given, and the scheme
 * turns those values into numbers. Asking for values rather than for a score is what keeps a hallucinated number from
 * ever reaching a consumer.
 * <p>
 * Both the vocabulary and the meaning of the declared context arrive as arguments, read from the catalog, so the
 * prompt is never assembled from constants that could drift away from what the scheme accepts.
 */
public interface SeverityReasoningModel {

    ModelSeverityProposal propose(Vulnerability vulnerability, List<ContextAttribute> context,
                                  List<SchemeMetric> vocabulary, String schemeId);

    /**
     * Derives a baseline vector from the description when the caller did not supply one. This is the weakest path of
     * the system — the anchor itself becomes an inference — which is why callers are encouraged to send the vector
     * and why the result is recorded as model derived with degraded confidence.
     */
    String deriveBaselineVector(Vulnerability vulnerability, String schemeId);

    String identifier();

    String promptVersion();
}
