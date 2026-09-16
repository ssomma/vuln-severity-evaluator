package org.challenge.vulnseverityevaluator.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;

/**
 * One metric of a scoring scheme, with the values it admits and their numeric weights.
 * <p>
 * This is the scoring specification as <em>data</em>, not as code. CVSS v3.1 is a published table of metrics, values
 * and coefficients: keeping it in the database means adding a scheme or correcting a weight is a row, and the
 * guidance the language model reads sits next to the values it is allowed to choose instead of being duplicated in a
 * prompt. It also removed the eight enums that used to carry the same table inside the calculator.
 * <p>
 * {@code BASE} metrics carry the weights. {@code CONTEXTUAL} metrics are the ones the model answers; those that
 * override a base metric point at it through {@code overrides} and borrow its weights, so no coefficient is stored
 * twice.
 */
@Entity
@Table(name = "scheme_metric")
public class SchemeMetric {

    @Id
    private String id;

    private String schemeId;

    private String code;

    private String label;

    private String guidance;

    @Enumerated(STRING)
    private Kind kind;

    private String overrides;

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

    public Kind kind() {
        return kind;
    }

    public Optional<String> overrides() {
        return Optional.ofNullable(overrides);
    }

    public int ordinal() {
        return ordinal;
    }

    public List<String> admitted() {
        return values.stream().map(MetricValue::code).toList();
    }

    public Optional<MetricValue> value(String code) {
        return values.stream().filter(value -> value.code().equals(code)).findFirst();
    }

    public enum Kind {

        BASE, CONTEXTUAL
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
