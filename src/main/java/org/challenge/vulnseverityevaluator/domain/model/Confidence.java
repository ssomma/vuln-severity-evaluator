package org.challenge.vulnseverityevaluator.domain.model;

/**
 * How much weight the evaluation deserves.
 * <p>
 * A plain vocabulary. The rules that choose a value live in the policy that decides them, not here, so the meaning
 * of each level stays stable while the thresholds behind it can be retuned.
 */
public enum Confidence {

    HIGH, MEDIUM, LOW
}
