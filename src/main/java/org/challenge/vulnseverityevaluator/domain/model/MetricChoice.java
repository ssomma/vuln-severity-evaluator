package org.challenge.vulnseverityevaluator.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A value chosen for one metric, with the reasoning behind it.
 * <p>
 * One type serves the whole path: it is what the language model proposes, what the assessment reports as its
 * justification, and what gets persisted. Splitting it in three would have produced three identical shapes and two
 * mappings that restate the same fields.
 */
@Embeddable
public class MetricChoice {

    private String metric;

    /**
     * Named explicitly because {@code value} is a reserved word in several databases, H2 among them, and the
     * generated DDL would not even parse.
     */
    @Column(name = "chosen_value")
    private String value;

    @Column(length = 2_000)
    private String rationale;

    protected MetricChoice() {
    }

    public static MetricChoice createMetricChoice(String metric, String value, String rationale) {
        MetricChoice choice = new MetricChoice();
        choice.metric = metric;
        choice.value = value;
        choice.rationale = rationale;
        return choice;
    }

    public String metric() {
        return metric;
    }

    public String value() {
        return value;
    }

    public String rationale() {
        return rationale;
    }
}
