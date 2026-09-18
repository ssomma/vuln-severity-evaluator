package org.challenge.vulnseverityevaluator.datasource.llm;

import org.challenge.vulnseverityevaluator.domain.model.*;
import org.challenge.vulnseverityevaluator.infrastructure.ModelAnswerUnusableException;
import org.challenge.vulnseverityevaluator.infrastructure.ModelProviderException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.function.Supplier;

import static java.util.stream.Collectors.joining;
import static org.challenge.vulnseverityevaluator.domain.model.MetricChoice.createMetricChoice;
import static org.challenge.vulnseverityevaluator.domain.model.ModelSeverityProposal.createModelSeverityProposal;
import static org.challenge.vulnseverityevaluator.infrastructure.metric.ApplicationMetricCollector.ResourceMetrics.collectResourceModelCall;

/**
 * Asks a language model, through Spring AI, for the contextual metric values of the active scheme.
 * <p>
 * Three decisions in here carry most of the risk handling:
 * <ul>
 * <li>The prompt is rendered from the metric catalog and the context catalog, not from constants. A correction to a
 * metric or a new admitted value changes what is asked and what is accepted at the same time.</li>
 * <li>Untrusted text never reaches the template. The vulnerability description travels as a template
 * <em>parameter</em>, so a description containing template syntax is substituted as data instead of being parsed as
 * part of the instruction.</li>
 * <li>The application name is deliberately not sent. The assessment does not need it, and withholding it keeps
 * internal inventory names out of an external provider.</li>
 * </ul>
 * The answer is mapped to a domain proposal that validates itself, so an answer outside the vocabulary fails here
 * rather than turning into a plausible looking score.
 */
public class LLMSeverityReasoningModel implements SeverityReasoningModel {

    static final String PROMPT_VERSION = "contextual-severity-v1";

    /**
     * The two questions this datasource asks, measured apart: asking for a baseline vector means the anchor of the
     * score was inferred instead of supplied, which is the weakest path of the system. They live here because this
     * is the only class that makes the distinction.
     */
    private static final String PROPOSAL_OPERATION = "proposal";
    private static final String BASELINE_VECTOR_OPERATION = "baseline_vector";

    private static final String SCHEME_PARAMETER = "schemeId";
    private static final String VOCABULARY_PARAMETER = "vocabulary";
    private static final String IDENTIFIER_PARAMETER = "identifier";
    private static final String DESCRIPTION_PARAMETER = "description";
    private static final String CONTEXT_PARAMETER = "context";
    private static final String LINE_BREAK = "\n";
    private static final String METRIC_LINE = "- %s (%s): %s Admitted values: %s.";
    private static final String CONTEXT_LINE = "- %s %s: %s";

    private final ChatClient chatClient;
    private final String model;
    private final Prompts prompts;

    public LLMSeverityReasoningModel(ChatClient chatClient, String model, Prompts prompts) {
        this.chatClient = chatClient;
        this.model = model;
        this.prompts = prompts;
    }

    @Override
    public ModelSeverityProposal propose(Vulnerability vulnerability, List<ContextAttribute> context,
                                         List<SchemeMetric> vocabulary, String schemeId) {
        ProposalAnswer answer = recorded(PROPOSAL_OPERATION, () -> chatClient.prompt()
                .system(spec -> spec.text(prompts.contextualSystem())
                        .param(SCHEME_PARAMETER, schemeId)
                        .param(VOCABULARY_PARAMETER, renderedVocabulary(vocabulary)))
                .user(spec -> spec.text(prompts.contextualUser())
                        .param(IDENTIFIER_PARAMETER, vulnerability.identifier())
                        .param(DESCRIPTION_PARAMETER, vulnerability.description())
                        .param(CONTEXT_PARAMETER, renderedContext(context)))
                .call()
                .entity(ProposalAnswer.class));
        try {
            return answer.toProposal(vocabulary);
        } catch (RuntimeException exception) {
            throw new ModelAnswerUnusableException("the model returned an invalid proposal", exception);
        }
    }

    @Override
    public String deriveBaselineVector(Vulnerability vulnerability, String schemeId) {
        VectorAnswer answer = recorded(BASELINE_VECTOR_OPERATION, () -> chatClient.prompt()
                .system(spec -> spec.text(prompts.baselineSystem()).param(SCHEME_PARAMETER, schemeId))
                .user(spec -> spec.text(prompts.baselineUser())
                        .param(IDENTIFIER_PARAMETER, vulnerability.identifier())
                        .param(DESCRIPTION_PARAMETER, vulnerability.description()))
                .call()
                .entity(VectorAnswer.class));
        return answer.vector();
    }

    /**
     * Records the outcome of the provider call. The failure path counts too: a timeout or a quota rejection is what
     * separates a failing provider from a failing service.
     */
    private <T> T recorded(String operation, Supplier<T> call) {
        try {
            T answer = call.get();
            collectResourceModelCall(this, operation, true);
            return answer;
        } catch (RuntimeException exception) {
            collectResourceModelCall(this, operation, false);
            throw new ModelProviderException("the model provider call failed", exception);
        }
    }

    @Override
    public String identifier() {
        return model;
    }

    @Override
    public String promptVersion() {
        return PROMPT_VERSION;
    }

    private static String renderedVocabulary(List<SchemeMetric> vocabulary) {
        return vocabulary.stream()
                .map(metric -> METRIC_LINE.formatted(metric.code(), metric.label(), metric.guidance(),
                        String.join(", ", metric.admitted())))
                .collect(joining(LINE_BREAK));
    }

    private static String renderedContext(List<ContextAttribute> context) {
        return context.stream()
                .map(attribute -> CONTEXT_LINE.formatted(attribute.kind(), attribute.code(), attribute.meaning()))
                .collect(joining(LINE_BREAK));
    }

    /**
     * The prompt resources of this datasource. Grouped so the constructor stays readable and so a second prompt
     * version can be wired in one place.
     */
    public record Prompts(Resource contextualSystem,
                          Resource contextualUser,
                          Resource baselineSystem,
                          Resource baselineUser) {
    }

    /**
     * Raw shape of the model answer. It stays inside this datasource: the domain never sees a provider payload, and
     * a provider that changes its shape cannot ripple past this class.
     */
    record ProposalAnswer(List<ChoiceAnswer> choices, String summary) {

        ModelSeverityProposal toProposal(List<SchemeMetric> vocabulary) {
            List<ChoiceAnswer> answered = CollectionUtils.isEmpty(choices) ? List.of() : choices;
            List<MetricChoice> mapped = answered.stream().map(ChoiceAnswer::toMetricChoice).toList();
            return createModelSeverityProposal(mapped, summary, vocabulary);
        }

        record ChoiceAnswer(String metric, String value, String rationale) {

            MetricChoice toMetricChoice() {
                return createMetricChoice(metric, value, rationale);
            }
        }
    }

    record VectorAnswer(String vector) {
    }
}
