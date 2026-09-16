package org.challenge.vulnseverityevaluator.domain.model;

/**
 * What an outage of the application costs the business, which is what makes an availability loss matter.
 * Its meaning is catalog data in {@link ContextAttribute}.
 */
public enum BusinessCriticality {

    TIER_1, TIER_2, TIER_3
}
