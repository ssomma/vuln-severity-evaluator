package org.challenge.vulnseverityevaluator.domain.model;

/**
 * The most sensitive kind of data the application handles, which is what makes a confidentiality loss matter.
 * Its meaning is catalog data in {@link ContextAttribute}.
 */
public enum DataClassification {

    CREDENTIALS, FINANCIAL, PII, INTERNAL, PUBLIC
}
