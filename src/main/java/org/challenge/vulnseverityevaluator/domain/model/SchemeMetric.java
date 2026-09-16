package org.challenge.vulnseverityevaluator.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static jakarta.persistence.FetchType.EAGER;
import static java.util.stream.Stream.concat;

/**
 * One metric of a scoring scheme, with the values it admits and their numeric weights.
 * <p>
 * This is the scoring specification as <em>data</em>, not as code. CVSS v3.1 is a published table of metrics, values
 * and coefficients: keeping it in the database means adding a scheme or correcting a weight is a row, and the
 * guidance the language model reads sits next to the values it is allowed to choose instead of being duplicated in a
 * prompt. It also removed the eight enums that used to carry the same table inside the calculator.
 * <p>
 * One row per metric, not one per scoring pass. A metric is scored twice — once from the baseline vector and once
 * from the declared context — but that is how the scheme computes, not what the scheme <em>is</em>: the weights, the
 * admitted values and the guidance are the same table read twice. Splitting it in two rows duplicated the vocabulary
 * on one side and the coefficients on the other, and needed a pointer between them to put the halves back together.
 */
@Entity
@Table(name = "scheme_metric")
public class SchemeMetric {

    /**
     * Abstention. Not stored as an admitted value: it is the protocol for "no evidence, keep the baseline", so it
     * belongs to every metric by construction and to none of their weight tables.
     */
    public static final String NOT_DEFINED = "X";

    @Id
    private String id;

    private String schemeId;

    private String code;

    private String label;

    private String guidance;

    private int ordinal;

    @ElementCollection(fetch = EAGER)
    @CollectionTable(name = "scheme_metric_value", joinColumns = @JoinColumn(name = "metric_id"))
    private List<MetricValue> values;

    protected SchemeMetric() {
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String guidance() {
        return guidance;
    }

    public int ordinal() {
        return ordinal;
    }

    /**
     * What the model may answer for this metric: every weighted value, plus the abstention.
     */
    public List<String> admitted() {
        return concat(values.stream().map(MetricValue::code), Stream.of(NOT_DEFINED)).toList();
    }

    public Optional<MetricValue> value(String code) {
        return values.stream().filter(value -> value.code().equals(code)).findFirst();
    }

    /**
     * An admitted value and what it is worth. {@code weightWhenScopeChanged} exists for the single CVSS metric whose
     * weight depends on Scope; for every other value it repeats the weight.
     */
    @Embeddable
    public static class MetricValue {

        private String code;

        private BigDecimal weight;

        private BigDecimal weightWhenScopeChanged;

        protected MetricValue() {
        }

        public String code() {
            return code;
        }

        public BigDecimal weight() {
            return weight;
        }

        public BigDecimal weight(boolean scopeChanged) {
            return scopeChanged ? weightWhenScopeChanged : weight;
        }
    }
}
