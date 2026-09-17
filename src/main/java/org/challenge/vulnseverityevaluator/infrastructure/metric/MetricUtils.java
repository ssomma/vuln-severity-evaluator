package org.challenge.vulnseverityevaluator.infrastructure.metric;

import java.math.BigDecimal;

import static java.util.Objects.isNull;

/**
 * The vocabulary of the metric convention: what can be measured ({@link Tags}) and how a value is named
 * ({@link Dimensions}).
 * <p>
 * Two closed enums rather than loose strings, so the whole inventory is readable in one file, a name cannot be
 * mistyped at a call site, and {@link Dimensions#mapDimension(Object)} is the single path every emitted value takes.
 * Emission belongs to {@link LoggerMetricCollector}.
 */
public class MetricUtils {

    private static final String ASSIGNMENT = ":";
    private static final String UNKNOWN = "unknown";

    private static final String DELTA_NONE = "none";
    private static final String DELTA_MINOR = "minor";
    private static final String DELTA_SIGNIFICANT = "significant";
    private static final String DIRECTION_UP = "up";
    private static final String DIRECTION_DOWN = "down";
    private static final String DIRECTION_FLAT = "flat";

    /**
     * Mirrors {@code EvaluationPolicy.SIGNIFICANT_DELTA} so the bucket a dashboard groups by lines up with the
     * threshold that forces a human review. Two constants in two layers, and nothing detects a divergence.
     */
    private static final BigDecimal SIGNIFICANT_DELTA = new BigDecimal("2.0");

    /**
     * Every metric this application emits. The prefix classifies what kind of question it answers:
     * <ul>
     * <li>{@code work.} — the steps of the flow this application exists to run, in {@code input} / {@code process} /
     * {@code output} stages.</li>
     * <li>{@code event.} — a business anomaly that is not the failure of a dependency.</li>
     * <li>{@code resource.} — an interaction with something outside the process.</li>
     * </ul>
     * The distinction that earns its keep is the last two: a provider answering 500 is a resource failure, a
     * provider answering 200 with an unusable body is an event.
     */
    public enum Tags {

        WORK_INPUT_EVALUATION_REQUEST("work.input.evaluation.request"),
        WORK_INPUT_EVALUATION_LOOKUP("work.input.evaluation.lookup"),
        WORK_INPUT_CONTEXT_ATTRIBUTE("work.input.context.attribute"),
        WORK_PROCESS_BASELINE_DERIVED("work.process.baseline.derived"),
        WORK_PROCESS_SEVERITY_ASSESSED("work.process.severity.assessed"),
        WORK_OUTPUT_EVALUATION_RECORDED("work.output.evaluation.recorded"),

        EVENT_MODEL_ANSWER_REJECTED("event.model.answer.rejected"),
        EVENT_CONTEXT_VALUE_REJECTED("event.context.value.rejected"),
        EVENT_SEVERITY_RATING_MOVED("event.severity.rating.moved"),
        EVENT_EVALUATION_FAILED("event.evaluation.failed"),

        RESOURCE_HTTP_INCOMING_REQUEST("resource.http.incoming.request"),
        RESOURCE_MODEL_CALL("resource.model.call");

        private final String tag;

        Tags(String tag) {
            this.tag = tag;
        }

        public String getTag() {
            return this.tag;
        }
    }

    /**
     * Every dimension this application emits: closed enums, catalog validated codes, booleans, bounded counts and
     * buckets. What must never appear here is listed in {@link ApplicationMetricCollector}.
     */
    public enum Dimensions {

        SCHEME("scheme"),
        BASELINE_SOURCE("baseline_source"),

        EXPOSURE("exposure"),
        DATA_CLASSIFICATION("data_classification"),
        BUSINESS_CRITICALITY("business_criticality"),
        RUNTIME_COUNT("runtime_count"),
        CONTROL_COUNT("control_count"),

        BASELINE_RATING("baseline_rating"),
        CONTEXTUAL_RATING("contextual_rating"),
        DIRECTION("direction"),
        DELTA("delta"),
        ABSTENTIONS("abstentions"),

        CONFIDENCE("confidence"),
        REVIEW_REQUIRED("review_required"),

        ATTRIBUTE_KIND("attribute_kind"),
        ATTRIBUTE_CODE("attribute_code"),

        ERROR_CLASS("error_class"),

        HTTP_METHOD("method"),
        HTTP_PATH("path"),
        HTTP_STATUS("status"),

        MODEL("model"),
        PROMPT_VERSION("prompt_version"),
        OPERATION("operation"),
        OUTCOME("outcome");

        private final String displayName;

        Dimensions(String dimension) {
            this.displayName = dimension;
        }

        public String mapDimension(Object dimensionValue) {
            return this.displayName.concat(ASSIGNMENT)
                    .concat(isNull(dimensionValue) ? UNKNOWN : dimensionValue.toString());
        }
    }

    static String delta(BigDecimal delta) {
        BigDecimal magnitude = delta.abs();
        return magnitude.signum() == 0
                ? DELTA_NONE
                : magnitude.compareTo(SIGNIFICANT_DELTA) >= 0 ? DELTA_SIGNIFICANT : DELTA_MINOR;
    }

    static String direction(BigDecimal delta) {
        int signum = delta.signum();
        return signum > 0 ? DIRECTION_UP : signum < 0 ? DIRECTION_DOWN : DIRECTION_FLAT;
    }
}
