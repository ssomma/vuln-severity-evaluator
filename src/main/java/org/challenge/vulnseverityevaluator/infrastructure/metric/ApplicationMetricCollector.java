package org.challenge.vulnseverityevaluator.infrastructure.metric;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.challenge.vulnseverityevaluator.datasource.llm.SeverityReasoningModel;
import org.challenge.vulnseverityevaluator.domain.model.*;
import org.challenge.vulnseverityevaluator.domain.model.ApplicationContext.RiskProfile;
import org.challenge.vulnseverityevaluator.infrastructure.metric.MetricUtils.Dimensions;
import org.challenge.vulnseverityevaluator.infrastructure.metric.MetricUtils.Tags;

import java.util.Objects;

import static org.challenge.vulnseverityevaluator.domain.model.BaselineVectorSource.CALLER_SUPPLIED;
import static org.challenge.vulnseverityevaluator.domain.model.BaselineVectorSource.MODEL_DERIVED;
import static org.challenge.vulnseverityevaluator.infrastructure.metric.LoggerMetricCollector.INSTANCE;
import static org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE;

/**
 * One {@code collect<MetricBeingCollected>} method per tag, grouped by the prefix of the tag it emits. The name is
 * the tag in PascalCase, so the correspondence holds in both directions: a log line names the method that emitted
 * it, and a call site names the series that is about to move.
 * <p>
 * Every method receives the object being measured rather than its fields. The mapping from an object to its
 * dimensions then lives here only, adding a dimension changes no signature, and no call site assembles a metric.
 * <p>
 * What is deliberately never a dimension, and why:
 * <ul>
 * <li>{@code vulnerability.identifier()} — the space of CVEs is unbounded.</li>
 * <li>{@code vulnerability.description()}, {@code proposal.summary()} and the vectors — free text, one from the
 * caller and one from the model.</li>
 * <li>{@code exception.getMessage()} — usually carries the raw model answer. Only the class is emitted.</li>
 * <li>{@code evaluation.id()} — unbounded by definition. An evaluation is retrieved by id, not grouped by it.</li>
 * <li>The <em>rejected</em> context code — the caller controls it and by definition it is not in the catalog, which
 * is why {@link EventMetrics#collectEventContextValueRejected} carries only the kind.</li>
 * <li>{@code context.name()} — internal inventory, which {@code LLMSeverityReasoningModel} deliberately
 * withholds from the model provider. Emitting it to an external observability backend would reintroduce exactly what
 * that class avoids.</li>
 * <li>The runtime and control sets joined into a string — combinatorial cardinality. They are emitted as counts, and
 * the detail goes to {@link WorkMetrics#collectWorkInputContextAttribute}, bounded by the catalog rows.</li>
 * </ul>
 */
public class ApplicationMetricCollector {

    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";

    public static class WorkMetrics {

        public static void collectWorkInputEvaluationRequest(Vulnerability vulnerability, ApplicationContext context,
                                                             String schemeId) {
            RiskProfile profile = context.riskProfile();
            INSTANCE.incrementCounter(Tags.WORK_INPUT_EVALUATION_REQUEST.getTag(),
                    Dimensions.SCHEME.mapDimension(schemeId),
                    Dimensions.BASELINE_SOURCE.mapDimension(
                            vulnerability.hasBaselineVector() ? CALLER_SUPPLIED : MODEL_DERIVED),
                    Dimensions.EXPOSURE.mapDimension(profile.exposure()),
                    Dimensions.DATA_CLASSIFICATION.mapDimension(profile.dataClassification()),
                    Dimensions.BUSINESS_CRITICALITY.mapDimension(profile.businessCriticality()),
                    Dimensions.RUNTIME_COUNT.mapDimension(context.runtime().size()),
                    Dimensions.CONTROL_COUNT.mapDimension(context.compensatingControls().size()));
        }

        public static void collectWorkInputEvaluationLookup(VulnerabilityEvaluation evaluation) {
            INSTANCE.incrementCounter(Tags.WORK_INPUT_EVALUATION_LOOKUP.getTag(),
                    Dimensions.SCHEME.mapDimension(evaluation.schemeId()),
                    Dimensions.CONTEXTUAL_RATING.mapDimension(evaluation.contextual().rating()),
                    Dimensions.CONFIDENCE.mapDimension(evaluation.confidence()),
                    Dimensions.REVIEW_REQUIRED.mapDimension(evaluation.reviewRequired()));
        }

        public static void collectWorkInputContextAttribute(ContextAttribute attribute) {
            INSTANCE.incrementCounter(Tags.WORK_INPUT_CONTEXT_ATTRIBUTE.getTag(),
                    Dimensions.ATTRIBUTE_KIND.mapDimension(attribute.kind()),
                    Dimensions.ATTRIBUTE_CODE.mapDimension(attribute.code()));
        }

        public static void collectWorkProcessBaselineDerived(String schemeId) {
            INSTANCE.incrementCounter(Tags.WORK_PROCESS_BASELINE_DERIVED.getTag(),
                    Dimensions.SCHEME.mapDimension(schemeId));
        }

        public static void collectWorkProcessSeverityAssessed(SeverityAssessment assessment,
                                                              ModelSeverityProposal proposal) {
            INSTANCE.incrementCounter(Tags.WORK_PROCESS_SEVERITY_ASSESSED.getTag(),
                    Dimensions.SCHEME.mapDimension(assessment.schemeId()),
                    Dimensions.BASELINE_RATING.mapDimension(assessment.baseline().rating()),
                    Dimensions.CONTEXTUAL_RATING.mapDimension(assessment.contextual().rating()),
                    Dimensions.DIRECTION.mapDimension(MetricUtils.direction(assessment.delta())),
                    Dimensions.DELTA.mapDimension(MetricUtils.delta(assessment.delta())),
                    Dimensions.ABSTENTIONS.mapDimension(proposal.abstentions()));
        }

        public static void collectWorkOutputEvaluationRecorded(VulnerabilityEvaluation evaluation) {
            INSTANCE.incrementCounter(Tags.WORK_OUTPUT_EVALUATION_RECORDED.getTag(),
                    Dimensions.SCHEME.mapDimension(evaluation.schemeId()),
                    Dimensions.CONTEXTUAL_RATING.mapDimension(evaluation.contextual().rating()),
                    Dimensions.BASELINE_SOURCE.mapDimension(evaluation.provenance().baselineVectorSource()),
                    Dimensions.CONFIDENCE.mapDimension(evaluation.confidence()),
                    Dimensions.REVIEW_REQUIRED.mapDimension(evaluation.reviewRequired()));
        }
    }

    public static class EventMetrics {

        public static void collectEventModelAnswerRejected(RuntimeException exception) {
            INSTANCE.incrementAnomalyCounter(Tags.EVENT_MODEL_ANSWER_REJECTED.getTag(),
                    Dimensions.ERROR_CLASS.mapDimension(name(exception)));
        }

        public static void collectEventContextValueRejected(ContextAttribute.Kind kind) {
            INSTANCE.incrementAnomalyCounter(Tags.EVENT_CONTEXT_VALUE_REJECTED.getTag(),
                    Dimensions.ATTRIBUTE_KIND.mapDimension(kind));
        }

        /**
         * Counted rather than reported as an anomaly: the context moving a score is the product of this service, not
         * a degradation. It is an {@code event} because a shift in its rate is a finding, not because it is wrong.
         */
        public static void collectEventSeverityRatingMoved(SeverityAssessment assessment) {
            if (assessment.baseline().rating() != assessment.contextual().rating()) {
                INSTANCE.incrementCounter(Tags.EVENT_SEVERITY_RATING_MOVED.getTag(),
                        Dimensions.SCHEME.mapDimension(assessment.schemeId()),
                        Dimensions.BASELINE_RATING.mapDimension(assessment.baseline().rating()),
                        Dimensions.CONTEXTUAL_RATING.mapDimension(assessment.contextual().rating()),
                        Dimensions.DIRECTION.mapDimension(MetricUtils.direction(assessment.delta())));
            }
        }

        public static void collectEventEvaluationFailed(RuntimeException exception) {
            INSTANCE.incrementAnomalyCounter(Tags.EVENT_EVALUATION_FAILED.getTag(),
                    Dimensions.ERROR_CLASS.mapDimension(name(exception)));
        }

        private static String name(RuntimeException exception) {
            return exception.getClass().getSimpleName();
        }
    }

    public static class ResourceMetrics {

        /**
         * The matched pattern, never the raw URI: the URI is caller controlled and would put identifiers into the
         * cardinality. An unmatched request has no pattern, and {@code unknown} is the right answer for it.
         */
        public static void collectResourceHttpIncomingRequest(HttpServletRequest request,
                                                              HttpServletResponse response) {
            INSTANCE.incrementCounter(Tags.RESOURCE_HTTP_INCOMING_REQUEST.getTag(),
                    Dimensions.HTTP_PATH.mapDimension(
                            Objects.toString(request.getAttribute(BEST_MATCHING_PATTERN_ATTRIBUTE), null)),
                    Dimensions.HTTP_METHOD.mapDimension(request.getMethod()),
                    Dimensions.HTTP_STATUS.mapDimension(response.getStatus()));
        }

        /**
         * The operation is named by the caller, because only the datasource knows which of its questions was being
         * asked. It is a bounded constant there, not caller input.
         */
        public static void collectResourceModelCall(SeverityReasoningModel model, String operation,
                                                    boolean succeeded) {
            INSTANCE.incrementCounter(Tags.RESOURCE_MODEL_CALL.getTag(),
                    Dimensions.MODEL.mapDimension(model.identifier()),
                    Dimensions.PROMPT_VERSION.mapDimension(model.promptVersion()),
                    Dimensions.OPERATION.mapDimension(operation),
                    Dimensions.OUTCOME.mapDimension(succeeded ? SUCCESS : FAILURE));
        }
    }
}
