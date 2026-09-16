package org.challenge.vulnseverityevaluator.infrastructure;

import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Counters for the three things worth watching in an AI assisted service: how often the model answers outside its
 * vocabulary, how often a cached evaluation is reused, and how many evaluations were produced.
 * <p>
 * The rejection counter is the important one. A rising rejection rate is the earliest visible symptom of model drift
 * or of a prompt that no longer matches the vocabulary, and without it the failure is silent.
 * <p>
 * Deliberately free of domain types: this layer sits below the business layers and must not reach into them.
 */
public class EvaluationMetrics {

    private static final Logger logger = getLogger(EvaluationMetrics.class);
    private static final String REJECTION_MESSAGE = "model answer rejected, total rejections {}";
    private static final String EVALUATION_MESSAGE = "evaluation produced, total {} with {} cache reuses";

    private final AtomicLong rejections = new AtomicLong();
    private final AtomicLong evaluations = new AtomicLong();
    private final AtomicLong cacheReuses = new AtomicLong();

    public void recordRejectedAnswer() {
        logger.warn(REJECTION_MESSAGE, rejections.incrementAndGet());
    }

    public void recordEvaluation(boolean reused) {
        evaluations.incrementAndGet();
        reuses(reused);
        logger.info(EVALUATION_MESSAGE, evaluations.get(), cacheReuses.get());
    }

    public long rejections() {
        return rejections.get();
    }

    public long evaluations() {
        return evaluations.get();
    }

    public long cacheReuses() {
        return cacheReuses.get();
    }

    private void reuses(boolean reused) {
        boolean counted = reused && cacheReuses.incrementAndGet() > 0;
        logger.debug("cache reuse recorded: {}", counted);
    }
}
