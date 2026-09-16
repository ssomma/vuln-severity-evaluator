package org.challenge.vulnseverityevaluator.domain.model;

/**
 * Where the baseline vector came from. This is the most consequential provenance field: a caller supplied vector
 * anchors the score to a hard datum, while a model derived one means the anchor itself is an inference.
 */
public enum BaselineVectorSource {

    CALLER_SUPPLIED, MODEL_DERIVED
}
