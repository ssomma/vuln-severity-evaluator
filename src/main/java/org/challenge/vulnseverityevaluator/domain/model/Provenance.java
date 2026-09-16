package org.challenge.vulnseverityevaluator.domain.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;

import java.time.Instant;

import static jakarta.persistence.EnumType.STRING;

/**
 * Under which conditions an evaluation was produced, and how much it can be relied on.
 * <p>
 * Without this an AI assisted score is unfalsifiable: two identical requests can differ because the model or the
 * prompt changed, and nothing would say so. The model and prompt version make drift detectable; the sources make it
 * clear which part of the result is datum and which is inference; the confidence and the review flag say when not to
 * act on the number.
 * <p>
 * Confidence and review live here rather than as separate fields because they answer the same question as the rest:
 * how should a consumer read this result. The rules that decide them are in the policy, not in this record.
 */
@Embeddable
public class Provenance {

    private static final String CALLER_DECLARED = "CALLER_DECLARED";

    private String model;

    private String promptVersion;

    /**
     * Persisted even though it has one possible value today, for the same reason the scheme identifier is: when a
     * second source appears, the records written before it stay interpretable.
     */
    private String contextSource;

    @Enumerated(STRING)
    private BaselineVectorSource baselineVectorSource;

    @Enumerated(STRING)
    private Confidence confidence;

    private boolean reviewRequired;

    private Instant evaluatedAt;

    protected Provenance() {
    }

    public static Provenance createProvenance(String model,
                                              String promptVersion,
                                              BaselineVectorSource baselineVectorSource,
                                              Confidence confidence,
                                              boolean reviewRequired) {
        Provenance provenance = new Provenance();
        provenance.model = model;
        provenance.promptVersion = promptVersion;
        provenance.contextSource = CALLER_DECLARED;
        provenance.baselineVectorSource = baselineVectorSource;
        provenance.confidence = confidence;
        provenance.reviewRequired = reviewRequired;
        provenance.evaluatedAt = Instant.now();
        return provenance;
    }

    public String model() {
        return model;
    }

    public String promptVersion() {
        return promptVersion;
    }

    public String contextSource() {
        return contextSource;
    }

    public BaselineVectorSource baselineVectorSource() {
        return baselineVectorSource;
    }

    public Confidence confidence() {
        return confidence;
    }

    public boolean reviewRequired() {
        return reviewRequired;
    }

    public Instant evaluatedAt() {
        return evaluatedAt;
    }
}
