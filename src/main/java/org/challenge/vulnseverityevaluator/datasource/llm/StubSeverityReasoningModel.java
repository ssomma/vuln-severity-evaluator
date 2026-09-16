package org.challenge.vulnseverityevaluator.datasource.llm;

import org.challenge.vulnseverityevaluator.domain.model.ContextAttribute;
import org.challenge.vulnseverityevaluator.domain.model.ContextAttribute.MetricSuggestion;
import org.challenge.vulnseverityevaluator.domain.model.MetricChoice;
import org.challenge.vulnseverityevaluator.domain.model.ModelSeverityProposal;
import org.challenge.vulnseverityevaluator.domain.model.SchemeMetric;
import org.challenge.vulnseverityevaluator.domain.model.Vulnerability;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toMap;
import static org.challenge.vulnseverityevaluator.domain.model.MetricChoice.createMetricChoice;
import static org.challenge.vulnseverityevaluator.domain.model.ModelSeverityProposal.createModelSeverityProposal;
import static org.challenge.vulnseverityevaluator.domain.model.SchemeMetric.NOT_DEFINED;

/**
 * Deterministic stand in for the language model, active in the {@code local} profile and in tests.
 * <p>
 * It exists for two reasons. It makes the service runnable end to end with no API key and no network, so the design
 * can be exercised and reviewed reproducibly. And it gives the tests a predictable collaborator, so a failing test
 * points at our code rather than at a model that answered differently today.
 * <p>
 * It holds no mapping table of its own: every declared attribute already carries the metric values it argues for, so
 * this class collects the suggestions of the catalog and answers X wherever the context said nothing. Moving the
 * specification into the database made the deterministic model almost free.
 */
public class StubSeverityReasoningModel implements SeverityReasoningModel {

    static final String IDENTIFIER = "stub-deterministic";
    static final String PROMPT_VERSION = "stub-v1";

    private static final String DERIVED_BASELINE_VECTOR = "CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H";
    private static final String SUMMARY =
            "Deterministic local evaluation derived from the declared context, without a language model.";
    private static final String SUGGESTED = "%s set to %s because %s.";
    private static final String ABSTAINED = "%s left as X: the declared context gives no evidence for it.";

    @Override
    public ModelSeverityProposal propose(Vulnerability vulnerability, List<ContextAttribute> context,
                                         List<SchemeMetric> vocabulary, String schemeId) {
        Map<String, ContextAttribute> sources = suggestions(context);
        List<MetricChoice> choices = vocabulary.stream()
                .map(metric -> choice(metric, sources))
                .toList();
        return createModelSeverityProposal(choices, SUMMARY, vocabulary);
    }

    @Override
    public String deriveBaselineVector(Vulnerability vulnerability, String schemeId) {
        return DERIVED_BASELINE_VECTOR;
    }

    @Override
    public String identifier() {
        return IDENTIFIER;
    }

    @Override
    public String promptVersion() {
        return PROMPT_VERSION;
    }

    private static Map<String, ContextAttribute> suggestions(List<ContextAttribute> context) {
        return context.stream()
                .flatMap(attribute -> attribute.suggestions().stream().map(suggestion -> Map.entry(suggestion, attribute)))
                .collect(toMap(entry -> entry.getKey().metric(), Map.Entry::getValue, (first, second) -> first));
    }

    private static MetricChoice choice(SchemeMetric metric, Map<String, ContextAttribute> sources) {
        return Optional.ofNullable(sources.get(metric.code()))
                .map(source -> suggested(metric, source))
                .orElseGet(() -> createMetricChoice(
                        metric.code(), NOT_DEFINED, ABSTAINED.formatted(metric.label())));
    }

    private static MetricChoice suggested(SchemeMetric metric, ContextAttribute source) {
        String value = source.suggestions().stream()
                .filter(suggestion -> suggestion.metric().equals(metric.code()))
                .map(MetricSuggestion::suggested)
                .findFirst()
                .orElse(NOT_DEFINED);
        return createMetricChoice(metric.code(), value, SUGGESTED.formatted(metric.label(), value, source.meaning()));
    }
}
