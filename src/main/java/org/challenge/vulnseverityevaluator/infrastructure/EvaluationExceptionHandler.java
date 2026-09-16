package org.challenge.vulnseverityevaluator.infrastructure;

import org.slf4j.Logger;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Turns failures into RFC 7807 problem responses.
 * <p>
 * It handles standard exception types rather than custom ones, which is what keeps this layer free of any dependency
 * on the domain — and the domain free of any dependency on this one. The distinction that matters survives anyway,
 * because the two conditions are genuinely different kinds of failure:
 * <ul>
 * <li>{@link IllegalArgumentException} — the caller sent something invalid: a malformed vector, a context value the
 * catalog does not know. Its message describes the caller's own input, so it comes back.</li>
 * <li>{@link IllegalStateException} — a collaborator produced something unusable, almost always a model answer
 * outside its vocabulary. The caller did nothing wrong, so it is not a 400, and the detail stays in the log.</li>
 * </ul>
 * Extending {@link ResponseEntityExceptionHandler} is not cosmetic: it keeps Spring's own handling of malformed
 * bodies, unknown enum values and type mismatches, which already carry the right status. A blanket handler without it
 * would swallow those and answer 502 to what is plainly a 400 — the controller tests caught exactly that.
 */
@RestControllerAdvice
public class EvaluationExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = getLogger(EvaluationExceptionHandler.class);

    private static final URI INVALID_REQUEST = URI.create("urn:problem:invalid-evaluation-request");
    private static final URI MODEL_UNUSABLE = URI.create("urn:problem:model-answer-unusable");

    private static final String INVALID_TITLE = "The evaluation request is not valid";
    private static final String UNUSABLE_TITLE = "The severity assessment could not be completed";
    private static final String UNUSABLE_DETAIL =
            "The reasoning model did not produce a usable answer. No severity was produced; retrying is safe.";

    private final EvaluationMetrics metrics;

    public EvaluationExceptionHandler(EvaluationMetrics metrics) {
        this.metrics = metrics;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidRequest(IllegalArgumentException exception) {
        logger.info("evaluation request rejected: {}", exception.getMessage());
        return problem(BAD_REQUEST.value(), INVALID_REQUEST, INVALID_TITLE, exception.getMessage());
    }

    /**
     * An unusable model answer is counted here, not just reported. A rising rejection rate is the earliest visible
     * symptom of model drift or of a prompt that no longer matches the vocabulary, and without the counter the
     * failure is silent.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleUnusableAnswer(IllegalStateException exception) {
        metrics.recordRejectedAnswer();
        logger.warn("model answer rejected: {}", exception.getMessage());
        return problem(BAD_GATEWAY.value(), MODEL_UNUSABLE, UNUSABLE_TITLE, UNUSABLE_DETAIL);
    }

    /**
     * Anything else coming out of the model call: a timeout, a provider error, a quota rejection. The cause is logged
     * and never returned, so a provider message cannot leak upstream configuration to a caller.
     */
    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleUnexpectedFailure(RuntimeException exception) {
        logger.error("severity evaluation failed", exception);
        return problem(BAD_GATEWAY.value(), MODEL_UNUSABLE, UNUSABLE_TITLE, UNUSABLE_DETAIL);
    }

    private static ProblemDetail problem(int status, URI type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(type);
        problem.setTitle(title);
        problem.setDetail(detail);
        return problem;
    }
}
