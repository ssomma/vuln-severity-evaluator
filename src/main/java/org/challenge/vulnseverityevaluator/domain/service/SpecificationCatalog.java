package org.challenge.vulnseverityevaluator.domain.service;

import org.challenge.vulnseverityevaluator.datasource.repository.ContextAttributeRepository;
import org.challenge.vulnseverityevaluator.datasource.repository.SchemeMetricRepository;
import org.challenge.vulnseverityevaluator.domain.model.ApplicationContext;
import org.challenge.vulnseverityevaluator.domain.model.ContextAttribute;
import org.challenge.vulnseverityevaluator.domain.model.ContextAttribute.Kind;
import org.challenge.vulnseverityevaluator.domain.model.SchemeMetric;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.challenge.vulnseverityevaluator.domain.model.ContextAttribute.Kind.BUSINESS_CRITICALITY;
import static org.challenge.vulnseverityevaluator.domain.model.ContextAttribute.Kind.COMPENSATING_CONTROL;
import static org.challenge.vulnseverityevaluator.domain.model.ContextAttribute.Kind.DATA_CLASSIFICATION;
import static org.challenge.vulnseverityevaluator.domain.model.ContextAttribute.Kind.EXPOSURE;
import static org.challenge.vulnseverityevaluator.domain.model.ContextAttribute.Kind.RUNTIME;
import static org.challenge.vulnseverityevaluator.domain.model.SchemeMetric.Kind.CONTEXTUAL;

/**
 * The catalogs the evaluation reads: the scoring specification and the admitted context values.
 * <p>
 * One collaborator instead of two repositories injected everywhere, because they are always used together — the
 * vocabulary shapes what the model may answer, and the context catalog shapes what the model is told. It is also the
 * one place that turns declared codes into catalog entries, which is where an unknown runtime or control is rejected.
 * <p>
 * A {@code @Component}: it is collaboration for the use case, not a use case, and presentation never calls it.
 */
@Component
public class SpecificationCatalog {

    private final SchemeMetricRepository metrics;
    private final ContextAttributeRepository attributes;

    public SpecificationCatalog(SchemeMetricRepository metrics, ContextAttributeRepository attributes) {
        this.metrics = metrics;
        this.attributes = attributes;
    }

    public List<SchemeMetric> specification(String schemeId) {
        return metrics.findBySchemeIdOrderByOrdinal(schemeId);
    }

    /**
     * What the model is allowed to answer: the contextual metrics of the scheme.
     */
    public static List<SchemeMetric> vocabulary(List<SchemeMetric> specification) {
        return specification.stream().filter(metric -> metric.kind() == CONTEXTUAL).toList();
    }

    /**
     * Turns the declared context into catalog entries, rejecting any code the catalog does not know. Rejecting is the
     * point: a silently dropped runtime would mean the model reasoned about a different application than the caller
     * described.
     */
    public List<ContextAttribute> resolve(ApplicationContext context) {
        List<ContextAttribute> resolved = new ArrayList<>();
        resolved.add(single(EXPOSURE, context.riskProfile().exposure().name()));
        resolved.add(single(DATA_CLASSIFICATION, context.riskProfile().dataClassification().name()));
        resolved.add(single(BUSINESS_CRITICALITY, context.riskProfile().businessCriticality().name()));
        resolved.addAll(many(RUNTIME, context.runtime()));
        resolved.addAll(many(COMPENSATING_CONTROL, context.compensatingControls()));
        return resolved;
    }

    private List<ContextAttribute> many(Kind kind, Set<String> codes) {
        return codes.stream().sorted().map(code -> single(kind, code)).toList();
    }

    private ContextAttribute single(Kind kind, String code) {
        return attributes.findByKind(kind).stream()
                .filter(attribute -> attribute.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(kind + " value not admitted: " + code));
    }
}
