package org.challenge.vulnseverityevaluator.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;

import java.math.BigDecimal;

import static jakarta.persistence.EnumType.STRING;

/**
 * A computed severity: the number, its qualitative rating, and the vector it was computed from.
 * <p>
 * The vector travels with the score on purpose. A number alone is not auditable — the vector is what lets a reviewer
 * recompute the result and disagree with it.
 * <p>
 * An embeddable, reused for the baseline and for the contextual score. One shape instead of six flat columns
 * repeated twice on the aggregate.
 */
@Embeddable
public class SeverityScore {

    public static final int VECTOR_MAX_LENGTH = 2_000;

    private BigDecimal score;

    @Enumerated(STRING)
    private SeverityRating rating;

    @Column(length = VECTOR_MAX_LENGTH)
    private String vector;

    protected SeverityScore() {
    }

    public static SeverityScore createSeverityScore(BigDecimal score, SeverityRating rating, String vector) {
        SeverityScore created = new SeverityScore();
        created.score = score;
        created.rating = rating;
        created.vector = vector;
        return created;
    }

    public BigDecimal score() {
        return score;
    }

    public SeverityRating rating() {
        return rating;
    }

    public String vector() {
        return vector;
    }
}
