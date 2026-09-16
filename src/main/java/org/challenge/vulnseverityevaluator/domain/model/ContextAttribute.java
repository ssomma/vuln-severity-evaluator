package org.challenge.vulnseverityevaluator.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.List;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.EAGER;

/**
 * One admitted value of the application context, with what it means and what it suggests.
 * <p>
 * A catalog in the database rather than enums in code, because these are open lists: runtimes and compensating
 * controls keep growing, and adding one should not be a deploy. The {@code meaning} is what the language model
 * actually reads — sending {@code TIER_1} alone would ask it to guess an internal convention — so the text that
 * shapes the prompt lives with the value it describes.
 * <p>
 * The suggestions are what makes the deterministic model possible without hardcoded mapping tables: each attribute
 * declares which metric values it argues for, and the local model simply collects them.
 */
@Entity
@Table(name = "context_attribute")
public class ContextAttribute {

    @Id
    private String id;

    @Enumerated(STRING)
    private Kind kind;

    private String code;

    private String meaning;

    @ElementCollection(fetch = EAGER)
    @CollectionTable(name = "context_suggestion", joinColumns = @JoinColumn(name = "attribute_id"))
    private List<MetricSuggestion> suggestions;

    protected ContextAttribute() {
    }

    public Kind kind() {
        return kind;
    }

    public String code() {
        return code;
    }

    public String meaning() {
        return meaning;
    }

    public List<MetricSuggestion> suggestions() {
        return List.copyOf(suggestions);
    }

    public enum Kind {

        EXPOSURE, DATA_CLASSIFICATION, BUSINESS_CRITICALITY, RUNTIME, COMPENSATING_CONTROL
    }

    /**
     * A metric value this attribute argues for, used by the deterministic model.
     */
    @Embeddable
    public static class MetricSuggestion {

        private String metric;

        private String suggested;

        protected MetricSuggestion() {
        }

        public String metric() {
            return metric;
        }

        public String suggested() {
            return suggested;
        }
    }
}
