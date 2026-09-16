package org.challenge.vulnseverityevaluator.domain.model;

/**
 * Qualitative scale shared by every scheme, so a consumer can act on a severity without knowing the scheme that
 * produced it. Which score maps to which rating is the scheme's business, not this enum's.
 */
public enum SeverityRating {

    NONE, LOW, MEDIUM, HIGH, CRITICAL
}
