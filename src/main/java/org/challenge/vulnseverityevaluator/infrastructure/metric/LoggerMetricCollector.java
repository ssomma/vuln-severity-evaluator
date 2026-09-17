package org.challenge.vulnseverityevaluator.infrastructure.metric;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * The single emitter, and the only object in this package. A metric is never produced by an instance someone creates:
 * the application calls a static {@code collect...} method of {@link ApplicationMetricCollector}, and that method
 * calls {@code INSTANCE}.
 * <p>
 * The logger is named {@code application.metric} rather than after this class, so the log backend can derive metrics
 * from that stream alone with nothing else falling into it. The rendered line —
 * {@code <tag> name:value name:value} — is what a Datadog collector consumes, so swapping this singleton for a real
 * one is a change of reference, not of the tags, the dimensions, the collector or any call site.
 */
public enum LoggerMetricCollector {

    INSTANCE;

    private static final Logger logger = getLogger("application.metric");

    private static final String LINE = "{} {}";
    private static final String SEPARATOR = " ";

    public void incrementCounter(String tag, String... dimensions) {
        logger.info(LINE, tag, render(dimensions));
    }

    /**
     * Same line at WARN, so the condition is visible in a log search without a dashboard. The only addition the
     * logger mechanism makes over a metric collector, and it is what makes an anomaly greppable.
     */
    public void incrementAnomalyCounter(String tag, String... dimensions) {
        logger.warn(LINE, tag, render(dimensions));
    }

    private static String render(String... dimensions) {
        return String.join(SEPARATOR, dimensions);
    }
}
