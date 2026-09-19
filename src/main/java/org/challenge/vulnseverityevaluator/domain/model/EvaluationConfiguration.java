package org.challenge.vulnseverityevaluator.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.util.Assert;

/**
 * The versioned configuration that makes an evaluation interpretable and participates in its fingerprint.
 * <p>
 * It has no identity or lifecycle apart from the evaluation that owns it, so it is both an immutable value object
 * and an embeddable persisted in the owning evaluation row.
 */
@Embeddable
public record EvaluationConfiguration(
        @Column(nullable = false) String model,
        @Column(nullable = false) String promptVersion,
        @Column(nullable = false) String catalogVersion,
        @Column(nullable = false) String policyVersion) {

    public EvaluationConfiguration {
        Assert.hasText(model, "the model identifier is required");
        Assert.hasText(promptVersion, "the prompt version is required");
        Assert.hasText(catalogVersion, "the catalog version is required");
        Assert.hasText(policyVersion, "the policy version is required");
    }

    public static EvaluationConfiguration createEvaluationConfiguration(String model,
                                                                        String promptVersion,
                                                                        String catalogVersion,
                                                                        String policyVersion) {
        return new EvaluationConfiguration(model, promptVersion, catalogVersion, policyVersion);
    }
}
