package org.challenge.vulnseverityevaluator.domain.model;

import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * What the language model proposes: a value for every contextual metric, each with its reasoning.
 * <p>
 * A value object of the domain whose job is to translate the answer of a datasource into something the domain can
 * trust. It validates itself against the metric catalog at construction time, so there is deliberately no way to
 * obtain an instance in an invalid state: an unknown metric, an unadmitted value or a missing answer is rejected,
 * never coerced into a default.
 * <p>
 * The model never proposes a number. Scores are computed by the scheme, so a hallucinated score cannot reach a
 * consumer even if the model volunteers one.
 * <p>
 * Failures are signalled with {@link IllegalStateException} rather than a custom type: a bad answer is not a bad
 * argument, it is an unusable state produced by a collaborator, and the caller who sent a perfectly valid request
 * should not be told its request was wrong.
 */
public record ModelSeverityProposal(List<MetricChoice> choices, String summary) {

    private static final String NOT_DEFINED = "X";

    public static ModelSeverityProposal createModelSeverityProposal(List<MetricChoice> choices,
                                                                    String summary,
                                                                    List<SchemeMetric> vocabulary) {
        Assert.state(!CollectionUtils.isEmpty(choices), "the model answer has no metric choices");
        Assert.state(StringUtils.hasText(summary), "the model answer has no summary");
        vocabulary.forEach(metric -> require(choices, metric));
        choices.forEach(choice -> validate(choice, vocabulary));
        return new ModelSeverityProposal(List.copyOf(choices), summary);
    }

    public String value(String metric) {
        return choice(metric).map(MetricChoice::value).orElseThrow();
    }

    /**
     * How often the model declined to commit. Many abstentions mean the declared context carried little evidence,
     * which is a reason to report lower confidence rather than to pretend the answer was informed.
     */
    public long abstentions() {
        return choices.stream().filter(choice -> NOT_DEFINED.equals(choice.value())).count();
    }

    private Optional<MetricChoice> choice(String metric) {
        return choices.stream().filter(choice -> choice.metric().equals(metric)).findFirst();
    }

    private static void require(List<MetricChoice> choices, SchemeMetric metric) {
        boolean answered = choices.stream().anyMatch(choice -> metric.code().equals(choice.metric()));
        Assert.state(answered, () -> "the model did not answer for metric " + metric.code());
    }

    private static void validate(MetricChoice choice, List<SchemeMetric> vocabulary) {
        SchemeMetric metric = vocabulary.stream()
                .filter(candidate -> candidate.code().equals(choice.metric()))
                .findFirst()
                .orElse(null);
        Assert.state(metric != null, () -> "unknown metric " + choice.metric());
        Assert.state(metric.admitted().contains(choice.value()),
                () -> "value " + choice.value() + " not admitted for " + metric.code());
    }
}
